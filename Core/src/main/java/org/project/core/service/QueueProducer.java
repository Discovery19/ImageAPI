package org.project.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.core.model.Mail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueProducer {
    @Value("${app.topic}")
    private String topic;
    private final KafkaTemplate<String, Mail> kafkaTemplate;

    public void sendMessage(Mail request) {
        kafkaTemplate.send(topic, request);
    }
}
