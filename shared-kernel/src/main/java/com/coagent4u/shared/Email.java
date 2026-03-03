package com.coagent4u.shared;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validated email address value object.
 */
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public Email {
        Objects.requireNonNull(value, "Email value must not be null");
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
