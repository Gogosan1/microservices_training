package com.example.file.converter.service.converters;

import com.example.file.converter.entity.ConversionResult;

import java.io.File;

public interface ConverterStrategy {

    public ConversionResult convert(File sourceFileToConverting, File targetFile);

    public boolean supports(String fileType);

}
