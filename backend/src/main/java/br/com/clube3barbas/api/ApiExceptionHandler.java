package br.com.clube3barbas.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        var detalhes = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados invalidos.", detalhes);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBusinessRule(IllegalArgumentException exception) {
        return resposta(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage(), java.util.List.of());
    }

    private ResponseEntity<ApiError> resposta(HttpStatus status, String mensagem, java.util.List<String> detalhes) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                detalhes
        ));
    }
}
