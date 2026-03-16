package kilian1111010.wealthandfinancetracker.exception;

import kilian1111010.wealthandfinancetracker.exception.exceptions.AlreadyRegisteredException;
import kilian1111010.wealthandfinancetracker.exception.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private String now() {
        return LocalDateTime.now().toString();
    }

    @ExceptionHandler(AlreadyRegisteredException.class)
    public ResponseEntity<Exception> alreadyRegistered(AlreadyRegisteredException e) {
        Exception error = new Exception(409, e.getMessage(), now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Exception> userNotFound(UserNotFoundException e) {
        Exception error = new Exception(404, e.getMessage(), now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
