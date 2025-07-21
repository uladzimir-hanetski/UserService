package org.example.userserv.exception;

public class InvalidSecurityParametersException extends RuntimeException {
    public InvalidSecurityParametersException(String message) {
        super(message);
    }
}
