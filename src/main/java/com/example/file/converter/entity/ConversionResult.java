package com.example.file.converter.entity;

import java.io.File;

public record ConversionResult(
        File file,
        String contentType,
        String extension
) {
}