package com.example.pdf.converter.kafka;

import com.example.pdf.converter.event.FileUploadedEvent;
import com.example.pdf.converter.service.DocumentManagementFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploadedKafkaConsumer {

    private final DocumentManagementFacade facade;

    @KafkaListener(topics = "${kafka.input-topic}")
    public void listen(FileUploadedEvent event) {

        facade.handle(event);

    }
}
