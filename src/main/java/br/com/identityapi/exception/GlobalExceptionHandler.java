package br.com.identityapi.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IdadeInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleIdadeInvalida(IdadeInvalidaException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("erro", ex.getMessage());
        body.put("detalhe", ex.getDetalhe());

        return ResponseEntity.status(440).body(body);
    }

    @ExceptionHandler(DocumentoIncompletoException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentoIncompleto(DocumentoIncompletoException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("erro", ex.getMessage());
        body.put("detalhe", "Verifique a qualidade das imagens e certifique-se de que todos os dados estão legíveis.");
        body.put("camposFaltantes", ex.getCamposFaltantes());

        return ResponseEntity.unprocessableEntity().body(body);
    }
}
