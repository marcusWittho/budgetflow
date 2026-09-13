package com.lwv.budgetflow.accounts.dto;

import java.util.List;
import java.util.UUID;

/**
 * Contas e formas de pagamento numa chamada so.
 *
 * O front precisa das duas listas ao abrir o formulario. Devolver juntas
 * evita duas viagens de rede para montar a mesma tela.
 */
public record LookupResponse(List<Item> accounts, List<Item> paymentMethods) {

    public record Item(UUID id, String name) {
    }
}
