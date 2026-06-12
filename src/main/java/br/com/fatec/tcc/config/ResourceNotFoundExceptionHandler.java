package br.com.fatec.tcc.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * Suprime o stack trace completo nas respostas 404 para recursos estáticos e
 * rotas inexistentes da API. Separado do GlobalExceptionHandler porque este é
 * escopado a @RestController e não captura exceções de ResourceHttpRequestHandler.
 */
@ControllerAdvice
public class ResourceNotFoundExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Recurso não encontrado"));
    }
}
