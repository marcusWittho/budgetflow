package com.lwv.budgetflow.ledger.service;

import com.lwv.budgetflow.ledger.entity.EntryEntity;
import com.lwv.budgetflow.ledger.entity.EntryGroupEntity;
import com.lwv.budgetflow.ledger.repository.EntryGroupRepository;
import com.lwv.budgetflow.ledger.repository.EntryRepository;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class EntryScheduleService {

    private final EntryRepository entryRepository;
    private final EntryGroupRepository groupRepository;

    @Transactional
    public List<EntryEntity> criarParcelado(EntryEntity primeira, int parcelaInicial, int total) {
        if (total < 1) {
            throw new IllegalArgumentException("O total de parcelas precisa ser ao menos 1.");
        }
        if (parcelaInicial < 1 || parcelaInicial > total) {
            throw new IllegalArgumentException(
                    "A parcela inicial precisa estar entre 1 e o total.");
        }

        UUID userId = primeira.getUserId();
        EntryGroupEntity grupo = EntryGroupEntity.installment(
                userId, proximoCodigo(userId, "P"), total, primeira.getDescription());
        groupRepository.save(grupo);

        List<EntryEntity> geradas = new ArrayList<>();

        for (int passo = 0, numero = parcelaInicial; numero <= total; passo++, numero++) {
            EntryEntity parcela = (passo == 0)
                    ? primeira
                    : primeira.copyForDate(
                            primeira.getEntryDate().plusMonths(passo), EntryEntity.PENDING);

            parcela.attachToGroup(grupo, numero);
            geradas.add(parcela);
        }

        return entryRepository.saveAll(geradas);
    }

    @Transactional
    public List<EntryEntity> criarRecorrente(EntryEntity primeira, int meses) {
        if (meses < 1) {
            throw new IllegalArgumentException("A recorrencia precisa gerar ao menos um mes.");
        }

        UUID userId = primeira.getUserId();
        EntryGroupEntity grupo = EntryGroupEntity.recurring(
                userId, proximoCodigo(userId, "R"), primeira.getDescription());
        groupRepository.save(grupo);

        return entryRepository.saveAll(materializar(primeira, grupo, meses, 1));
    }

    @Transactional
    public int estenderRecorrente(EntryGroupEntity grupo, int horizonteMeses, LocalDate hoje) {
        if (!grupo.isRecurring() || !grupo.isActive()) {
            return 0;
        }

        EntryEntity ultima = entryRepository.findTopByGroupOrderByEntryDateDesc(grupo).orElse(null);
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

        EntryEntity semente = ultima.copyForDate(
                ultima.getEntryDate().plusMonths(1), EntryEntity.PENDING);

        entryRepository.saveAll(
                materializar(semente, grupo, (int) faltando, proximoNumero));

        return (int) faltando;
    }

    @Transactional
    public int cancelarFuturos(EntryGroupEntity grupo, LocalDate depoisDe) {
        List<EntryEntity> condenados = entryRepository
                .findByGroupAndStatusAndEntryDateAfter(grupo, EntryEntity.PENDING, depoisDe);

        entryRepository.deleteAll(condenados);
        grupo.deactivate();
        groupRepository.save(grupo);

        return condenados.size();
    }

    private List<EntryEntity> materializar(EntryEntity semente, EntryGroupEntity grupo,
                                           int meses, int numeroInicial) {
        List<EntryEntity> geradas = new ArrayList<>(meses);

        for (int passo = 0; passo < meses; passo++) {
            EntryEntity linha = (passo == 0)
                    ? semente
                    : semente.copyForDate(
                            semente.getEntryDate().plusMonths(passo), EntryEntity.PENDING);

            linha.attachToGroup(grupo, numeroInicial + passo);
            geradas.add(linha);
        }

        return geradas;
    }

    private String proximoCodigo(UUID userId, String prefixo) {
        int maior = groupRepository.maiorNumeroDoCodigo(userId, prefixo + "%");
        return "%s%03d".formatted(prefixo, maior + 1);
    }
}
