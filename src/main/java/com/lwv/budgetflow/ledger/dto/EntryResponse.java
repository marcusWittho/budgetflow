package com.lwv.budgetflow.ledger.dto;

import com.lwv.budgetflow.ledger.entity.EntryEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * O que sai no JSON.
 *
 * Os nomes vao junto com os ids para o front nao precisar de uma segunda
 * chamada so para montar a linha da tabela.
 *
 * E este DTO existe tambem por seguranca: devolver a entidade direto
 * significa que qualquer campo adicionado nela no futuro vaza no JSON.
 */
public record EntryResponse(
        UUID id,
        LocalDate entryDate,
        UUID categoryId,
        String categoryName,
        String categoryType,
        UUID subcategoryId,
        String subcategoryName,
        String subcategoryNature,
        UUID accountId,
        String accountName,
        UUID paymentMethodId,
        String paymentMethodName,
        String description,
        BigDecimal amount,
        String status,
        String notes,
        String groupCode,
        String installmentLabel
) {

    public static EntryResponse from(EntryEntity e) {
        var sub = e.getSubcategory();
        var conta = e.getAccount();
        var forma = e.getPaymentMethod();

        return new EntryResponse(
                e.getId(),
                e.getEntryDate(),
                e.getCategory().getId(),
                e.getCategory().getName(),
                e.getCategory().getType(),
                sub == null ? null : sub.getId(),
                sub == null ? null : sub.getName(),
                sub == null ? null : sub.getNature(),
                conta == null ? null : conta.getId(),
                conta == null ? null : conta.getName(),
                forma == null ? null : forma.getId(),
                forma == null ? null : forma.getName(),
                e.getDescription(),
                e.getAmount(),
                e.getStatus(),
                e.getNotes(),
                e.getGroup() == null ? null : e.getGroup().getCode(),
                e.installmentLabel());
    }
}
