package kilian1111010.wealthandfinancetracker.exception;

public record Exception(
        int status,
        String message,
        String timestamp
) {}
