package org.example.userserv.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ValueAlreadyExistsException extends RuntimeException {
    public ValueAlreadyExistsException(String field, String value) {
        super("Field '" + field + "' with value '" + value + "' already exists");
    }
}