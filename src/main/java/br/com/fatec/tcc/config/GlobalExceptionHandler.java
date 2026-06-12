package br.com.fatec.tcc.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException e) {
        String msg = (e.getMessage() != null && !e.getMessage().isBlank()) ? e.getMessage() : "Acesso negado";
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", msg));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(Map.of("message", errors));
    }

    /**
     * Exceções de regra de negócio (RuntimeException) têm mensagens controladas pelos
     * services — podem ser exibidas ao usuário. Mensagens muito longas indicam exceções
     * inesperadas e são substituídas por mensagem genérica para evitar vazamento interno.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "Erro inesperado";
        if (msg.length() > 300) {
            log.error("RuntimeException inesperada com mensagem longa", e);
            msg = "Erro inesperado no servidor";
        } else {
            log.warn("Regra de negócio violada: {}", msg);
        }
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        log.error("Erro interno não tratado", e);
        return ResponseEntity.internalServerError()
                .body(Map.of("message", "Erro interno no servidor"));
    }
}
