package com.coagent4u.shared;

import java.util.Objects;

/** Validated email address value object. */
public record Email(String value) {
    private static final java.util.regex.Pattern EMAIL_PATTERN = java.util.regex.Pattern
            .compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        Objects.requireNonNull(value, "Email value must not be null");
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email address: " + value);
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
