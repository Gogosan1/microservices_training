package com.example.pdf.converter.service;


import com.example.pdf.converter.entity.ConversionResultMetadata;
import com.example.pdf.converter.service.converters.ConverterStrategy;
import com.example.pdf.converter.util.exceptions.UnsupportedFileTypeException;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConverterService {

    private final List<ConverterStrategy> converterStrategyList;
    private final Tika tika;

    public ConversionResultMetadata convert(File sourceFile, File targetFile) {

        try {
            String fileType = tika.detect(sourceFile);


            ConverterStrategy converter = converterStrategyList.stream()
                    .filter(strategy -> strategy.supports(fileType))
                    .findFirst()
                    .orElseThrow(() ->
                            new UnsupportedFileTypeException("Can not convert file to pdf!" +
                                    " Files with type: " + fileType + " is not supported!")
                    );

            return converter.convert(sourceFile, targetFile);

        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

    }
}
