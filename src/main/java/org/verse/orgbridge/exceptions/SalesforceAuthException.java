package org.verse.orgbridge.exceptions;

import lombok.Getter;

@Getter
public class SalesforceAuthException extends RuntimeException {

    private final String code;

    public SalesforceAuthException(String code, String message) {
        super(message);
        this.code = code;
    }

}
