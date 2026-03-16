package kilian1111010.wealthandfinancetracker.domain.user;

import kilian1111010.wealthandfinancetracker.auth.RegisterDto;

public interface UserService {

    UserEntity createUser(RegisterDto dto);

    UserEntity authenticate(String email, String password);
}
