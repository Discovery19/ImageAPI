package org.project.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;

public record AuthRequest(@JsonProperty("email") @Email(message = "Email should be valid") String email, @JsonProperty("password") String password) {
}
