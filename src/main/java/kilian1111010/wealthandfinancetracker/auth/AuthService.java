package kilian1111010.wealthandfinancetracker.auth;

import jakarta.servlet.http.HttpSession;
import kilian1111010.wealthandfinancetracker.domain.user.UserEntity;
import kilian1111010.wealthandfinancetracker.domain.user.UserService;
import kilian1111010.wealthandfinancetracker.exception.exceptions.AlreadyRegisteredException;
import kilian1111010.wealthandfinancetracker.exception.exceptions.InvalidCredentialsException;
import kilian1111010.wealthandfinancetracker.exception.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class AuthService {

    private final UserService userService;

    ResponseEntity<LoginResponse> register(RegisterDto dto, HttpSession session) {
        try {
            UserEntity userEntity = this.userService.createUser(dto);
            createSession(session, userEntity.getId(), userEntity.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new LoginResponse(userEntity.getId(), userEntity.getUsername(), true));
        } catch (AlreadyRegisteredException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new LoginResponse(null, null, false));
        }
    }

    ResponseEntity<LoginResponse> login(LoginDto dto, HttpSession session) {
        try {
            UserEntity userEntity  = this.userService.authenticate(dto.email(), dto.password());
            createSession(session, userEntity.getId(), userEntity.getUsername());
            return ResponseEntity.ok(new LoginResponse(userEntity.getId(), userEntity.getUsername(), true));
        } catch (UserNotFoundException | InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(null, null, false));
        }
    }

    ResponseEntity<Boolean> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(true);
    }

    private void createSession(HttpSession session, UUID userId, String userName) {
        session.setAttribute("userId", userId);
        session.setAttribute("userName", userName);
    }
}
