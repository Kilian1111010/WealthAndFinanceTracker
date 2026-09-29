package kilian1111010.wealthandfinancetracker.exception;

import kilian1111010.wealthandfinancetracker.exception.exceptions.AlreadyRegisteredException;
import kilian1111010.wealthandfinancetracker.exception.exceptions.InvalidCredentialsException;
import kilian1111010.wealthandfinancetracker.exception.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

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

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Exception> invalidCredentials(InvalidCredentialsException e) {
        Exception error = new Exception(401, e.getMessage(), now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Exception> invalidInput(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        Exception error = new Exception(400, message, now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
