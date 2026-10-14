package com.example.pdf.converter.kafka;

import com.example.pdf.converter.event.FileUploadedEvent;
import com.example.pdf.converter.service.DocumentManagementFacade;
import com.example.pdf.converter.service.persistence.PersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploadedKafkaConsumer {

    private final DocumentManagementFacade facade;
    private final PersistenceService persistenceService;

    @KafkaListener(topics = "${kafka.input-topic}")
    public void listen(FileUploadedEvent event, Acknowledgment ask) {

        facade.validateEvent(event);

        log.info("Событие пришло {}", event.pathToFile());
        if (!persistenceService.tryRegisterInInbox(event.id())) {
            log.warn("Событие: {} с таким ID:{} уже есть!", event, event.id());
            ask.acknowledge();
            return;
        }

        ask.acknowledge();
        facade.handle(event);

    }
}
