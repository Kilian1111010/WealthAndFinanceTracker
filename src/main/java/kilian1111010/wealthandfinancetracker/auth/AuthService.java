package kilian1111010.wealthandfinancetracker.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kilian1111010.wealthandfinancetracker.domain.user.UserEntity;
import kilian1111010.wealthandfinancetracker.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class AuthService {

    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    LoginResponse register(RegisterDto dto, HttpServletRequest request, HttpServletResponse response) {
        UserEntity userEntity = this.userService.createUser(dto);
        signIn(userEntity, request, response);
        return new LoginResponse(userEntity.getId(), userEntity.getUsername());
    }

    LoginResponse login(LoginDto dto, HttpServletRequest request, HttpServletResponse response) {
        UserEntity userEntity = this.userService.authenticate(dto.username(), dto.password());
        signIn(userEntity, request, response);
        return new LoginResponse(userEntity.getId(), userEntity.getUsername());
    }

    Optional<LoginResponse> currentUser(@Nullable UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return this.userService.findById(userId)
                .map(userEntity -> new LoginResponse(userEntity.getId(), userEntity.getUsername()));
    }

    private void signIn(UserEntity userEntity, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(userEntity.getId(), null, List.of());

        this.sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

        SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        this.securityContextHolderStrategy.setContext(context);
        this.securityContextRepository.saveContext(context, request, response);
    }
}
