package com.lwv.budgetflow.ledger.controller;

import com.lwv.budgetflow.ledger.dto.EntryRequest;
import com.lwv.budgetflow.ledger.dto.EntryResponse;
import com.lwv.budgetflow.ledger.service.EntryService;
import com.lwv.budgetflow.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * O userId NUNCA vem do corpo do request — sai sempre do UserPrincipal que
 * o JwtAuthenticationFilter colocou no contexto. Se viesse do payload,
 * qualquer um gravaria lancamento na conta alheia.
 *
 * Nao precisa de configuracao no SecurityConfig: o anyRequest()
 * .authenticated() ja cobre este caminho.
 */
@RestController
@RequestMapping("/api/entries")
@RequiredArgsConstructor
public class EntryController {

    private final EntryService service;

    /**
     * Cria lancamento avulso, compra parcelada ou assinatura.
     * Devolve todas as linhas geradas — num 12x, sao doze.
     *
     * 201 Created e o status correto para criacao, nao 200.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<EntryResponse> criar(@AuthenticationPrincipal UserPrincipal principal,
                                     @RequestBody EntryRequest pedido) {
        return service.criar(principal.getId(), pedido)
                .stream().map(EntryResponse::from).toList();
    }

    /**
     * Lancamentos do periodo.
     *
     * Sem parametro: mes corrente.
     * ?month=2026-03      : aquele mes.
     * ?de=...&ate=...     : intervalo livre, util para o dashboard.
     */
    @GetMapping
    public List<EntryResponse> listar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {

        UUID userId = principal.getId();

        List<com.lwv.budgetflow.ledger.entity.EntryEntity> lancamentos = (de != null && ate != null)
                ? service.listarPeriodo(userId, de, ate)
                : service.listarMes(userId, month == null ? YearMonth.now() : month);

        return lancamentos.stream().map(EntryResponse::from).toList();
    }

    @PatchMapping("/{id}/pagar")
    public EntryResponse marcarPago(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable UUID id) {
        return EntryResponse.from(service.marcarPago(principal.getId(), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@AuthenticationPrincipal UserPrincipal principal,
                       @PathVariable UUID id) {
        service.apagar(principal.getId(), id);
    }
}
