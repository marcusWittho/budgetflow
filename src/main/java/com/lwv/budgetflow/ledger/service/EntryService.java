package com.lwv.budgetflow.ledger.service;

import com.lwv.budgetflow.accounts.repository.AccountRepository;
import com.lwv.budgetflow.accounts.repository.PaymentMethodRepository;
import com.lwv.budgetflow.ledger.domain.Entry;
import com.lwv.budgetflow.ledger.repository.EntryRepository;
import com.lwv.budgetflow.ledger.web.EntryRequest;
import com.lwv.budgetflow.taxonomy.domain.Category;
import com.lwv.budgetflow.taxonomy.domain.Subcategory;
import com.lwv.budgetflow.taxonomy.repository.CategoryRepository;
import com.lwv.budgetflow.taxonomy.repository.SubcategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Onde as regras de lancamento moram.
 *
 * O controller nao deve conter regra: ele traduz HTTP e delega. Assim a
 * mesma logica serve para um endpoint, um importador de planilha ou um job.
 *
 * TODO metodo recebe userId como primeiro parametro. Nenhuma busca aqui
 * acontece sem escopo — e o que impede um assinante de anexar a categoria
 * de outro passando o UUID dela no corpo do request.
 */
@Service
public class EntryService {

    private final EntryRepository entryRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final AccountRepository accountRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final EntryScheduleService scheduleService;

    public EntryService(EntryRepository entryRepository,
                        CategoryRepository categoryRepository,
                        SubcategoryRepository subcategoryRepository,
                        AccountRepository accountRepository,
                        PaymentMethodRepository paymentMethodRepository,
                        EntryScheduleService scheduleService) {
        this.entryRepository = entryRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.accountRepository = accountRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.scheduleService = scheduleService;
    }

    /**
     * Cria lancamento avulso, compra parcelada ou assinatura.
     * Devolve as linhas gravadas — uma no caso avulso, varias nos outros.
     */
    @Transactional
    public List<Entry> criar(UUID userId, EntryRequest pedido) {
        validar(pedido);
        Entry lancamento = montar(userId, pedido);

        if (pedido.repeticao() == null || pedido.repeticao().isBlank()) {
            return List.of(entryRepository.save(lancamento));
        }

        return switch (pedido.repeticao()) {
            case "installment" -> {
                if (pedido.totalParcelas() == null) {
                    throw new IllegalArgumentException(
                            "Compra parcelada exige o total de parcelas.");
                }
                yield scheduleService.criarParcelado(
                        lancamento, pedido.parcelaInicialOuPadrao(), pedido.totalParcelas());
            }
            case "recurring" -> scheduleService.criarRecorrente(
                    lancamento, pedido.mesesOuPadrao());
            default -> throw new IllegalArgumentException(
                    "Repeticao desconhecida: " + pedido.repeticao());
        };
    }

    /**
     * readOnly = true avisa o Hibernate que nada vai mudar, entao ele nao
     * precisa rastrear alteracoes. Economia pequena mas gratuita.
     */
    @Transactional(readOnly = true)
    public List<Entry> listarMes(UUID userId, YearMonth periodo) {
        return entryRepository.findPeriodo(
                userId, periodo.atDay(1), periodo.atEndOfMonth());
    }

    @Transactional(readOnly = true)
    public List<Entry> listarPeriodo(UUID userId, LocalDate de, LocalDate ate) {
        if (de.isAfter(ate)) {
            throw new IllegalArgumentException("A data inicial e posterior a final.");
        }
        return entryRepository.findPeriodo(userId, de, ate);
    }

    @Transactional
    public Entry marcarPago(UUID userId, UUID id) {
        Entry lancamento = exigir(userId, id);
        lancamento.markPaid();
        return lancamento;
    }

    /**
     * Apaga um lancamento. Se ele faz parte de um grupo, apaga so esta
     * linha — as outras parcelas continuam. Para encerrar o grupo inteiro
     * existe o cancelarFuturos no EntryScheduleService.
     */
    @Transactional
    public void apagar(UUID userId, UUID id) {
        entryRepository.delete(exigir(userId, id));
    }

    /**
     * Sempre findByIdAndUserId, nunca findById cru. Se o lancamento existe
     * mas pertence a outro assinante, o resultado e 404 — que e a resposta
     * correta: quem pergunta nao deve nem saber que aquele id existe.
     */
    private Entry exigir(UUID userId, UUID id) {
        return entryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Lancamento nao encontrado."));
    }

    private void validar(EntryRequest p) {
        if (p.entryDate() == null) {
            throw new IllegalArgumentException("A data e obrigatoria.");
        }
        if (p.categoryId() == null) {
            throw new IllegalArgumentException("A categoria e obrigatoria.");
        }
        if (p.description() == null || p.description().isBlank()) {
            throw new IllegalArgumentException("A descricao e obrigatoria.");
        }
        if (p.amount() == null || p.amount().signum() <= 0) {
            throw new IllegalArgumentException("O valor precisa ser maior que zero.");
        }
        if (!Entry.PAID.equals(p.status()) && !Entry.PENDING.equals(p.status())) {
            throw new IllegalArgumentException("Status deve ser 'paid' ou 'pending'.");
        }
    }

    private Entry montar(UUID userId, EntryRequest p) {
        Category categoria = categoryRepository.findByIdAndUserId(p.categoryId(), userId)
                .orElseThrow(() -> new EntityNotFoundException("Categoria nao encontrada."));

        Entry lancamento = new Entry(userId, p.entryDate(), categoria,
                p.description().trim(), p.amount(), p.status());
        lancamento.setNotes(p.notes());

        if (p.subcategoryId() != null) {
            Subcategory sub = subcategoryRepository
                    .findByIdAndCategoryUserId(p.subcategoryId(), userId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Subcategoria nao encontrada."));

            // Sem esta checagem seria possivel gravar "Aluguel" (de Moradia)
            // dentro de "Transporte", e o dashboard por categoria ficaria
            // inconsistente sem ninguem perceber.
            if (!sub.getCategory().getId().equals(categoria.getId())) {
                throw new IllegalArgumentException(
                        "A subcategoria nao pertence a categoria informada.");
            }
            lancamento.setSubcategory(sub);
        }

        if (p.accountId() != null) {
            lancamento.setAccount(accountRepository
                    .findByIdAndUserId(p.accountId(), userId)
                    .orElseThrow(() -> new EntityNotFoundException("Conta nao encontrada.")));
        }

        if (p.paymentMethodId() != null) {
            lancamento.setPaymentMethod(paymentMethodRepository
                    .findByIdAndUserId(p.paymentMethodId(), userId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Forma de pagamento nao encontrada.")));
        }

        return lancamento;
    }
}
