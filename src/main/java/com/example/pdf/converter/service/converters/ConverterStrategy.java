package com.example.pdf.converter.service.converters;

import com.example.pdf.converter.entity.ConversionResultMetadata;

import java.io.File;

public interface ConverterStrategy {

    ConversionResultMetadata convert(File sourceFileToConverting, File targetFile);

    boolean supports(String fileType);

}
