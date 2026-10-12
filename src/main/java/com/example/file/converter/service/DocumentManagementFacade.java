package com.example.file.converter.service;

import com.example.file.converter.entity.ConversionResult;
import com.example.file.converter.event.FileUploadedEvent;
import com.example.file.converter.repository.storage.MinioStorage;
import com.example.file.converter.service.persistence.PersistenceService;
import com.example.file.converter.util.exceptions.InvalidInputEventException;
import com.example.file.converter.util.exceptions.PermanentProcessingException;
import com.example.file.converter.util.exceptions.ProcessingErrorCode;
import com.example.file.converter.util.exceptions.RetryableProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentManagementFacade {

    private final ConverterService converterService;
    private final PersistenceService persistenceService;
    private final MinioStorage storage;


    // TODO: продумать логику ретраев для файлов, неуспевшихся сохраниться, и долго(сколько?)
    //  висящих со статусом: PROCESSING
    // TODO: нужна ли логика ретраев для FAILURE событий? Или просто писать их сразу в outbox как FAILURE
    // TODO: вынести всю работу с файлами в отдельный сервис
    // TODO: добавить commit offset при успешных обработках, а также сделать мехнанизм retry и DLT
    // TODO: настроить debezium чтобы он наконец-то смог публиковать события в kafka
    public void handle(FileUploadedEvent event) {
        validateEvent(event);

        log.info("Событие пришло {}", event.pathToFile());
        if (!persistenceService.tryStartProcessing(event.id())) {
            log.warn("Событие: {} с таким ID:{} уже есть!", event, event.id());
            return;
        }

        File initialTempFile = null;
        File outputTempFile = null;
        ConversionResult conversionResult;

        try {

            initialTempFile = downloadToTempFile(event.pathToFile());
            outputTempFile = createTempOutputFile();

            conversionResult = converterService.convert(initialTempFile, outputTempFile);
            log.info("Файл успешно конвертировался:{}", event.pathToFile());

            String resultObjectKey = determineTargetMinioPath(event.pathToFile(), conversionResult.extension());

            storage.upload(outputTempFile, resultObjectKey, conversionResult.contentType());
            log.info("Конвертированный файл: {} успешно загрузился в S3", resultObjectKey);
            persistenceService.success(event.id(), resultObjectKey);

        } catch (PermanentProcessingException exception) {
            log.error(exception.getPublicMessage());
            try {
                persistenceService.failure(event.id(), exception);
            } catch (Exception persistenceException) {
                exception.addSuppressed(persistenceException);
                persistenceService.releaseForRetry(event.id());
                throw new RetryableProcessingException(
                        ProcessingErrorCode.INTERNAL_ERROR,
                        "Не удалось сохранить событие о терминальной ошибке обработки",
                        exception
                );

            }
        } catch (RetryableProcessingException exception) {
            log.error("Повторим попытку:{}", exception.getMessage());
            persistenceService.releaseForRetry(event.id());
            throw exception;

        } catch (Exception exception) {
            persistenceService.releaseForRetry(event.id());
            throw new RetryableProcessingException(
                    ProcessingErrorCode.INTERNAL_ERROR,
                    "Внутренняя ошибка при обработке файла; требуется повторная попытка",
                    exception
            );

        } finally {
            cleanUpTempFile(initialTempFile);
            cleanUpTempFile(outputTempFile);
        }
    }

    private void validateEvent(FileUploadedEvent event) {
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


    private File downloadToTempFile(String pathToFile) throws Exception {
        File tempFile = File.createTempFile("minio_incoming_", ".tmp");
        try (InputStream inputStream = storage.download(pathToFile);
             FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
            inputStream.transferTo(fileOutputStream);
        }
        log.info("Файл из S3:{} скачан во временный файл:{}", pathToFile, tempFile.toPath());
        return tempFile;
    }

    private File createTempOutputFile() {
        try {
            return File.createTempFile("converted_output_", ".tmp");
        } catch (IOException exception) {
            throw new RetryableProcessingException(
                    ProcessingErrorCode.TEMP_FILE_IO_FAILED,
                    "Не удалось создать временный файл результата конвертации",
                    exception
            );
        }
    }

    private void cleanUpTempFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
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
