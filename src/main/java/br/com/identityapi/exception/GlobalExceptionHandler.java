package br.com.identityapi.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // HTTP 440 não é padrão no Spring, então usamos status 400 com código customizado
    // ou forçamos o status 440 diretamente
    @ExceptionHandler(IdadeInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleIdadeInvalida(IdadeInvalidaException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("erro", ex.getMessage());
        body.put("detalhe", ex.getDetalhe());

        return ResponseEntity.status(440).body(body);
    }
}
