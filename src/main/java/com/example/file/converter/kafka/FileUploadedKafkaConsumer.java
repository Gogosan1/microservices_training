package com.example.file.converter.kafka;

import com.example.file.converter.event.FileUploadedEvent;
import com.example.file.converter.service.DocumentManagementFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileUploadedKafkaConsumer {

    private final DocumentManagementFacade facade;

    @KafkaListener(topics = "${kafka.input-topic}")
    public void listen(FileUploadedEvent event) {
        facade.handle(event);
    }
}
