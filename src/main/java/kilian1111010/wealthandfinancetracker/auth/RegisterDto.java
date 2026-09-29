package kilian1111010.wealthandfinancetracker.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDto(
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 255) String username
) {}
