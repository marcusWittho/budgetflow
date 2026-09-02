package com.lwv.budgetflow.ledger.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Corpo do POST /api/entries.
 *
 * O mesmo payload cobre os tres casos da tela. Quem decide e "repeticao":
 *
 *   ausente        -> lancamento unico
 *   "installment"  -> exige totalParcelas
 *   "recurring"    -> usa meses, ou 6 se omitido
 *
 * Quando ha parcelamento, "amount" e o valor de CADA parcela — o numero
 * que aparece na fatura — e nao o total da compra.
 *
 * Repare que nao existe userId aqui. Ele sai do token, nunca do corpo:
 * se viesse do payload, qualquer um gravaria na conta alheia.
 */
public record EntryRequest(
        LocalDate entryDate,
        UUID categoryId,
        UUID subcategoryId,
        UUID accountId,
        UUID paymentMethodId,
        String description,
        BigDecimal amount,
        String status,
        String notes,
        String repeticao,
        Integer totalParcelas,
        Integer parcelaInicial,
        Integer meses
) {

    public int parcelaInicialOuPadrao() {
        return parcelaInicial == null ? 1 : parcelaInicial;
    }

    public int mesesOuPadrao() {
        return meses == null ? 6 : meses;
    }
}
