package com.lwv.budgetflow.shared.web;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz excecao em resposta HTTP decente.
 *
 * Sem isso, todo erro sai como 500. Uma categoria inexistente e culpa de
 * quem chamou (404), nao falha do servidor — e o front precisa dessa
 * distincao para mostrar a mensagem certa.
 *
 * @RestControllerAdvice vale para TODOS os controllers da aplicacao.
 *
 * NOTA: se o SecurityConfig nao tiver "/error" no permitAll, qualquer erro
 * interno e redirecionado para lá, bate na autenticacao e volta como 401 —
 * mascarando a causa real. Vale conferir.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(EntityNotFoundException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> pedidoInvalido(IllegalArgumentException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> conflito(DataIntegrityViolationException e) {
        // A mensagem do banco cita nome de constraint e de coluna. Vazar
        // isso para o cliente entrega detalhe do schema, entao devolvemos
        // algo genErico e deixamos o detalhe no log.
        log.warn("Violacao de integridade", e);
        return resposta(HttpStatus.CONFLICT, "Esse registro conflita com um existente.");
    }

    private ResponseEntity<Map<String, Object>> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "message", mensagem == null ? status.getReasonPhrase() : mensagem));
    }
}
