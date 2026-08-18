package org.verse.orgbridge.model;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Action {

    RETRIEVE("retrieve"),
    DEPLOY("deploy"),
    VALIDATE("validate");

    private final String value;

    Action(String value) {
        this.value = value;
    }

    public static Action fromValue(String value) {
        return Arrays.stream(values())
                .filter(a -> a.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid action: " + value)
                );
    }
}
