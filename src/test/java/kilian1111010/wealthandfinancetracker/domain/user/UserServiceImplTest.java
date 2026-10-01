package kilian1111010.wealthandfinancetracker.domain.user;

import kilian1111010.wealthandfinancetracker.auth.RegisterDto;
import kilian1111010.wealthandfinancetracker.exception.exceptions.AlreadyRegisteredException;
import kilian1111010.wealthandfinancetracker.exception.exceptions.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        this.userService = new UserServiceImpl(this.userRepository, this.passwordEncoder);
        when(this.userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createUserStoresHashedPassword() {
        UserEntity user = this.userService.createUser(new RegisterDto("secret-password", "alice"));

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getPassword()).isNotEqualTo("secret-password");
        assertThat(this.passwordEncoder.matches("secret-password", user.getPassword())).isTrue();
    }

    @Test
    void createUserRejectsExistingUsername() {
        when(this.userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> this.userService.createUser(new RegisterDto("secret-password", "alice")))
                .isInstanceOf(AlreadyRegisteredException.class);
        verify(this.userRepository, never()).save(any());
    }

    @Test
    void createUserMapsUniqueViolationToAlreadyRegistered() {
        when(this.userRepository.save(any(UserEntity.class))).thenThrow(new DataIntegrityViolationException("UQ_USER_NAME"));

        assertThatThrownBy(() -> this.userService.createUser(new RegisterDto("secret-password", "alice")))
                .isInstanceOf(AlreadyRegisteredException.class);
    }

    @Test
    void authenticateReturnsUserForCorrectPassword() {
        UserEntity stored = storedUser("alice", "secret-password");
        when(this.userRepository.findByUsername("alice")).thenReturn(Optional.of(stored));

        assertThat(this.userService.authenticate("alice", "secret-password")).isSameAs(stored);
    }

    @Test
    void authenticateRejectsWrongPassword() {
        when(this.userRepository.findByUsername("alice")).thenReturn(Optional.of(storedUser("alice", "secret-password")));

        assertThatThrownBy(() -> this.userService.authenticate("alice", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void authenticateRejectsUnknownUser() {
        when(this.userRepository.findByUsername("bob")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.userService.authenticate("bob", "secret-password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private UserEntity storedUser(String username, String rawPassword) {
        return UserEntity.builder()
                .username(username)
                .password(this.passwordEncoder.encode(rawPassword))
                .build();
    }
}
