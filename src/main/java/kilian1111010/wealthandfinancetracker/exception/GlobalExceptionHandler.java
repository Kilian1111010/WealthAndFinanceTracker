package kilian1111010.wealthandfinancetracker.exception;

import kilian1111010.wealthandfinancetracker.exception.exceptions.AlreadyRegisteredException;
import kilian1111010.wealthandfinancetracker.exception.exceptions.InvalidCredentialsException;
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
    public ResponseEntity<ApiError> alreadyRegistered(AlreadyRegisteredException e) {
        ApiError error = new ApiError(409, e.getMessage(), now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> invalidCredentials(InvalidCredentialsException e) {
        ApiError error = new ApiError(401, e.getMessage(), now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidInput(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ApiError error = new ApiError(400, message, now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
