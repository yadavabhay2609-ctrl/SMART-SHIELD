package com.smartshield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AiChatMessage {

    @NotBlank(message = "Message role is required")
    @Pattern(regexp = "^(user|assistant|model)$", message = "Role must be 'user' or 'assistant'")
    private String role;

    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 3000, message = "Message content cannot exceed 3000 characters")
    private String content;

    public AiChatMessage() {
    }

    public AiChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
