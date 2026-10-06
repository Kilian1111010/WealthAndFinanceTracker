package kilian1111010.wealthandfinancetracker.domain.user;

import kilian1111010.wealthandfinancetracker.auth.RegisterDto;
import kilian1111010.wealthandfinancetracker.exception.exceptions.AlreadyRegisteredException;
import kilian1111010.wealthandfinancetracker.exception.exceptions.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserEntity createUser(RegisterDto dto) {

        if (this.userRepository.existsByUsername(dto.username())) {
            throw new AlreadyRegisteredException();
        }

        UserEntity userEntity = UserEntity.builder()
                .password(Objects.requireNonNull(this.passwordEncoder.encode(dto.password())))
                .username(dto.username())
                .build();

        try {
            return this.userRepository.save(userEntity);
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyRegisteredException();
        }
    }

    @Override
    public UserEntity authenticate(String username, String rawPassword) {

        UserEntity userEntity = this.userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        if (!this.passwordEncoder.matches(rawPassword, userEntity.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return userEntity;
    }

    @Override
    public Optional<UserEntity> findById(UUID id) {
        return this.userRepository.findById(id);
    }
}
