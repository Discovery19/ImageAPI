package org.project.mail.dto;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
@Data
public class Mail {
    private String toAddress;
    @Value("${app.mail.username}")
    private String subject;
    private String message;
}
