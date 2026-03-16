package kilian1111010.wealthandfinancetracker.auth;

import java.util.UUID;

public record LoginResponse(
        UUID userId,
        String name,
        boolean success
) {}