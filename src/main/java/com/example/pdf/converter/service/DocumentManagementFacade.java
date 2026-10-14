package com.example.pdf.converter.service;

import com.example.pdf.converter.entity.ConversionResultMetadata;
import com.example.pdf.converter.event.FileUploadedEvent;
import com.example.pdf.converter.infrastructure.storage.MinioStorage;
import com.example.pdf.converter.service.persistence.PersistenceService;
import com.example.pdf.converter.util.exceptions.InvalidInputEventException;
import com.example.pdf.converter.util.exceptions.PermanentProcessingException;
import com.example.pdf.converter.util.exceptions.RetryableProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentManagementFacade {

    private final ConverterService converterService;
    private final PersistenceService persistenceService;
    private final MinioStorage storage;
    private final FileDiskService fileDiskService;


    // TODO: внутренняя логика ретраев для зависших в PROCESSING событий пока отсутствует
    // TODO: сделать сервис обработчик асинхронным и сделать пул потоков для обработки
    public void handle(FileUploadedEvent event) {

        File initialTempFile = null;
        File outputTempFile = null;

        try {

            initialTempFile = fileDiskService.downloadToTempFile(event.pathToFile(), storage);
            outputTempFile = fileDiskService.createTempOutputFile();

            ConversionResultMetadata conversionResultMetadata = converterService.convert(initialTempFile, outputTempFile);
            log.info("Файл успешно конвертировался:{}", event.pathToFile());

            String resultObjectKey = determineTargetMinioPath(event.pathToFile(), conversionResultMetadata.extension());

            storage.upload(outputTempFile, resultObjectKey, conversionResultMetadata.contentType());
            log.info("Конвертированный файл: {} успешно загрузился в S3", resultObjectKey);

            persistenceService.success(event.id(), resultObjectKey);

        } catch (PermanentProcessingException exception) {
            log.error("Критическая ошибка обработки файла: {}", exception.getPublicMessage());
            persistenceService.markAsPermanentFailure(event.id(), exception);

        } catch (RetryableProcessingException exception) {
            log.warn("Временная ошибка. Будет выполнен ретрай: {}", exception.getMessage());
            persistenceService.releaseForRetry(event.id());
            throw exception;

        } catch (Exception exception) {
            log.error("Непредвиденная ошибка при обработке файла; требуется повторная попытка", exception);
            persistenceService.releaseForRetry(event.id());

        } finally {
            fileDiskService.cleanUpTempFile(initialTempFile);
            fileDiskService.cleanUpTempFile(outputTempFile);
        }
    }

    public void validateEvent(FileUploadedEvent event) {
        if (event == null) {
            throw new InvalidInputEventException("Kafka-событие не должно быть null");
        }
        if (event.id() == null) {
            throw new InvalidInputEventException("В Kafka-событии отсутствует id");
        }

        if (event.pathToFile() == null || event.pathToFile().isBlank()) {
            throw new InvalidInputEventException("В Kafka-событии не указан ключ исходного объекта");
        }

        log.info("Событие:{} прошло валидацию", event);
    }


    private String determineTargetMinioPath(String sourceObjectKey, String outputExtension) {
        int slashIndex = sourceObjectKey.lastIndexOf('/');
        int dotIndex = sourceObjectKey.lastIndexOf('.');
        String baseKey = dotIndex > slashIndex
                ? sourceObjectKey.substring(0, dotIndex)
                : sourceObjectKey;
        String normalizedExtension = outputExtension.startsWith(".")
                ? outputExtension
                : "." + outputExtension;

        String resultObjectKey = baseKey + "_converted" + normalizedExtension;
        log.info("Ключ для нового файла в S3: {}", resultObjectKey);
        return resultObjectKey;
    }

}
