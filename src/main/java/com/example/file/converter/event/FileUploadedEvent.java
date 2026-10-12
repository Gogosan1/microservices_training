package com.example.file.converter.event;

import java.util.UUID;

public record FileUploadedEvent(
        UUID id,
        String pathToFile
) {
}
