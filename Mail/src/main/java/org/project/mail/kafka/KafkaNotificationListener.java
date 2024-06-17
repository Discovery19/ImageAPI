package org.project.mail.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.mail.dto.Mail;
import org.project.mail.service.EmailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaNotificationListener {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = "${app.topic}",
            groupId = "mail",
            containerFactory = "kafkaListenerContainerFactory")
    public void sendMail(@Payload String notification) throws JsonProcessingException, MessagingException {
        Mail mail = objectMapper.readValue(notification, Mail.class);
        emailService.sendSimpleEmail(mail.getToAddress(), mail.getSubject(), mail.getToAddress());
    }

}

