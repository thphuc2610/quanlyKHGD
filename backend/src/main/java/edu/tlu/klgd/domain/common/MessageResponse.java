package edu.tlu.klgd.domain.common;

import java.time.Instant;
import java.util.List;

public record MessageResponse(
    int status,
    String message,
    String path,
    Instant timestamp,
    List<String> errors
) {
    public MessageResponse(int status, String message, String path) {
        this(status, message, path, Instant.now(), List.of());
    }

    public MessageResponse(int status, String message, String path, List<String> errors) {
        this(status, message, path, Instant.now(), errors == null ? List.of() : errors);
    }
}
