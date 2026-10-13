package com.example.file.converter.entity;

import java.io.File;

public record ConversionResultMetadata(
        String contentType,
        String extension
) {
}