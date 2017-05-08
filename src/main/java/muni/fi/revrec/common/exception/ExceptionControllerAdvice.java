package muni.fi.revrec.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@ControllerAdvice
public class ExceptionControllerAdvice {
    @ResponseBody
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<?> entityNotFoundExceptionHandler(Exception ex) {
        return ResponseEntity.ok(ex.getMessage());
    }
}
