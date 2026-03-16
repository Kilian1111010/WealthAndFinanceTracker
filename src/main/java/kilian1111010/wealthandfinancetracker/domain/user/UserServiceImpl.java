package kilian1111010.wealthandfinancetracker.domain.user;

import kilian1111010.wealthandfinancetracker.auth.RegisterDto;
import kilian1111010.wealthandfinancetracker.exception.exceptions.AlreadyRegisteredException;
import kilian1111010.wealthandfinancetracker.exception.exceptions.InvalidCredentialsException;
import kilian1111010.wealthandfinancetracker.exception.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
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
                .id(UUID.randomUUID())
                .password(Objects.requireNonNull(this.passwordEncoder.encode(dto.password())))
                .username(dto.username())
                .build();

        return this.userRepository.save(userEntity);
    }

    @Override
    public UserEntity authenticate(String name, String rawPassword) {

        Optional<UserEntity> userEntityOpt = this.userRepository.findByUsernameAndPassword(name, Objects.requireNonNull(this.passwordEncoder.encode(rawPassword)));
        UserEntity userEntity = userEntityOpt.orElseThrow(UserNotFoundException::new);


        if (!this.passwordEncoder.matches(rawPassword, userEntity.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return userEntity;
    }
}
