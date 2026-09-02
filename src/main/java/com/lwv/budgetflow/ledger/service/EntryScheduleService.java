package com.lwv.budgetflow.ledger.service;

import com.lwv.budgetflow.ledger.domain.Entry;
import com.lwv.budgetflow.ledger.domain.EntryGroup;
import com.lwv.budgetflow.ledger.repository.EntryGroupRepository;
import com.lwv.budgetflow.ledger.repository.EntryRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Porte da geracao de parcelas e recorrencias do app em Python.
 *
 * A decisao central foi mantida: cada parcela e cada mensalidade vira uma
 * LINHA REAL, com data futura e status pendente. E o que faz projecao de
 * fluxo de caixa ser um "where entry_date between" em vez de calculo.
 *
 * LocalDate.plusMonths ja faz o clamp de dia que o somar_meses fazia com
 * calendar.monthrange: compra em 31/01 cai em 28/02 (ou 29 em bissexto).
 */
@Service
public class EntryScheduleService {

    private final EntryRepository entryRepository;
    private final EntryGroupRepository groupRepository;

    public EntryScheduleService(EntryRepository entryRepository,
                                EntryGroupRepository groupRepository) {
        this.entryRepository = entryRepository;
        this.groupRepository = groupRepository;
    }

    /**
     * Grava uma compra parcelada inteira de uma vez.
     *
     * O amount em "primeira" e o valor de CADA parcela — o numero que
     * aparece na fatura — e nao o total da compra. Mesma convencao do app
     * em Python.
     *
     * Aceita comecar no meio: parcelaInicial=9, total=21 grava da 9 a 21.
     * Serve para lancar um parcelamento que ja corria antes do sistema.
     */
    @Transactional
    public List<Entry> criarParcelado(Entry primeira, int parcelaInicial, int total) {
        if (total < 1) {
            throw new IllegalArgumentException("O total de parcelas precisa ser ao menos 1.");
        }
        if (parcelaInicial < 1 || parcelaInicial > total) {
            throw new IllegalArgumentException(
                    "A parcela inicial precisa estar entre 1 e o total.");
        }

        UUID userId = primeira.getUserId();
        EntryGroup grupo = EntryGroup.installment(
                userId, proximoCodigo(userId, "P"), total, primeira.getDescription());
        groupRepository.save(grupo);

        List<Entry> geradas = new ArrayList<>();

        for (int passo = 0, numero = parcelaInicial; numero <= total; passo++, numero++) {
            // Cada parcela soma meses a data ORIGINAL, nunca a parcela
            // anterior. Somando de uma em uma, uma compra em 31/01 viraria
            // 31/01, 28/02, 28/03 — travada no dia 28 para sempre.
            Entry parcela = (passo == 0)
                    ? primeira
                    : primeira.copyForDate(
                            primeira.getEntryDate().plusMonths(passo), Entry.PENDING);

            parcela.attachToGroup(grupo, numero);
            geradas.add(parcela);
        }

        return entryRepository.saveAll(geradas);
    }

    /**
     * Cria uma assinatura e materializa os proximos "meses" meses.
     *
     * No Python isso tinha horizonte fixo de 6 meses e um botao
     * "renovar +6". Aqui quem estende e o job, e o usuario nao clica nada.
     */
    @Transactional
    public List<Entry> criarRecorrente(Entry primeira, int meses) {
        if (meses < 1) {
            throw new IllegalArgumentException("A recorrencia precisa gerar ao menos um mes.");
        }

        UUID userId = primeira.getUserId();
        EntryGroup grupo = EntryGroup.recurring(
                userId, proximoCodigo(userId, "R"), primeira.getDescription());
        groupRepository.save(grupo);

        return entryRepository.saveAll(materializar(primeira, grupo, meses, 1));
    }

    /**
     * Estende uma assinatura ativa ate ter horizonteMeses meses gravados a
     * frente de hoje. Idempotente: rodar duas vezes no mesmo dia nao
     * duplica, porque o calculo parte da ultima linha existente.
     */
    @Transactional
    public int estenderRecorrente(EntryGroup grupo, int horizonteMeses, LocalDate hoje) {
        if (!grupo.isRecurring() || !grupo.isActive()) {
            return 0;
        }

        Entry ultima = entryRepository.findTopByGroupOrderByEntryDateDesc(grupo).orElse(null);
        if (ultima == null) {
            return 0;
        }

        long faltando = ChronoUnit.MONTHS.between(
                YearMonth.from(ultima.getEntryDate()),
                YearMonth.from(hoje.plusMonths(horizonteMeses)));

        if (faltando <= 0) {
            return 0;
        }

        int proximoNumero = (ultima.getInstallmentNumber() == null
                ? 0 : ultima.getInstallmentNumber()) + 1;

        Entry semente = ultima.copyForDate(
                ultima.getEntryDate().plusMonths(1), Entry.PENDING);

        entryRepository.saveAll(
                materializar(semente, grupo, (int) faltando, proximoNumero));

        return (int) faltando;
    }

    /**
     * Encerra uma assinatura, ou desfaz as parcelas futuras de uma compra.
     *
     * Lancamentos ja pagos ficam: sao historico, nao previsao. Apagar o que
     * ja saiu da conta bagunçaria o fechamento dos meses anteriores.
     */
    @Transactional
    public int cancelarFuturos(EntryGroup grupo, LocalDate depoisDe) {
        List<Entry> condenados = entryRepository
                .findByGroupAndStatusAndEntryDateAfter(grupo, Entry.PENDING, depoisDe);

        entryRepository.deleteAll(condenados);
        grupo.deactivate();
        groupRepository.save(grupo);

        return condenados.size();
    }

    private List<Entry> materializar(Entry semente, EntryGroup grupo,
                                     int meses, int numeroInicial) {
        List<Entry> geradas = new ArrayList<>(meses);

        for (int passo = 0; passo < meses; passo++) {
            Entry linha = (passo == 0)
                    ? semente
                    : semente.copyForDate(
                            semente.getEntryDate().plusMonths(passo), Entry.PENDING);

            linha.attachToGroup(grupo, numeroInicial + passo);
            geradas.add(linha);
        }

        return geradas;
    }

    /**
     * Proximo P001 / R001 do usuario.
     *
     * Isto e um max+1, entao existe corrida teorica. A constraint
     * UNIQUE (user_id, code) e a rede de seguranca: em colisao a transacao
     * falha e o usuario tenta de novo. Com uma pessoa escrevendo por vez
     * nunca acontece; se um dia acontecer, troque por uma sequence.
     */
    private String proximoCodigo(UUID userId, String prefixo) {
        int maior = groupRepository.maiorNumeroDoCodigo(userId, prefixo + "%");
        return "%s%03d".formatted(prefixo, maior + 1);
    }
}
