package kilian1111010.wealthandfinancetracker.exception;

public record ApiError(
        int status,
        String message,
        String timestamp
) {}
