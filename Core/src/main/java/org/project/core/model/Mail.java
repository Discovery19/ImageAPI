package org.project.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Mail {
    @JsonProperty("toAddress")
    private String toAddress;
    @JsonProperty("message")
    private String message;
}
