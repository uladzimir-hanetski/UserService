package org.example.userserv.exception;

public class ValueAlreadyExistsException extends RuntimeException {
    public ValueAlreadyExistsException(String field, String value) {
        super("Field '" + field + "' with value '" + value + "' already exists");
    }
}