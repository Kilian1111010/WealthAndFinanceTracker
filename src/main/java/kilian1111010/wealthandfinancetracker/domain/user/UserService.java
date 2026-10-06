package kilian1111010.wealthandfinancetracker.domain.user;

import kilian1111010.wealthandfinancetracker.auth.RegisterDto;

import java.util.Optional;
import java.util.UUID;

public interface UserService {

    UserEntity createUser(RegisterDto dto);

    UserEntity authenticate(String username, String password);

    Optional<UserEntity> findById(UUID id);
}
