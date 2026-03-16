package kilian1111010.wealthandfinancetracker.auth;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginDto dto, HttpSession httpSession) {
        return authService.login(dto, httpSession);
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterDto dto, HttpSession httpSession) {
        return authService.register(dto, httpSession);
    }

    @PostMapping("/logout")
    public ResponseEntity<Boolean> logout(HttpSession httpSession) {
        return authService.logout(httpSession);
    }
}

