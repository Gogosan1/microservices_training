package com.example.file.converter.service.converters;

import com.example.file.converter.entity.ConversionResultMetadata;

import java.io.File;

public interface ConverterStrategy {

    public ConversionResultMetadata convert(File sourceFileToConverting, File targetFile);

    public boolean supports(String fileType);

}
