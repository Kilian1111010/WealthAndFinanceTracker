package kilian1111010.wealthandfinancetracker.exception.exceptions;

public class AlreadyRegisteredException extends RuntimeException {

    public AlreadyRegisteredException() {
        super("User already registered");
    }
}
