package org.project.core.dto;

public record LoginResponse(String token, long expiresIn) {
}
