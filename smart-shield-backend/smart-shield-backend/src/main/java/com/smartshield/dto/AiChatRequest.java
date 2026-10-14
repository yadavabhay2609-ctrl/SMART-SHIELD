package com.smartshield.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiChatRequest {

    @NotBlank(message = "Question or message cannot be empty")
    @Size(max = 3000, message = "Message length cannot exceed 3000 characters")
    private String message;

    @Size(max = 6, message = "Conversation history cannot exceed 6 previous turns")
    @Valid
    private List<AiChatMessage> history = new ArrayList<>();

    public AiChatRequest() {
    }

    public AiChatRequest(String message) {
        this.message = message;
    }

    public AiChatRequest(String message, List<AiChatMessage> history) {
        this.message = message;
        this.history = history != null ? history : new ArrayList<>();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<AiChatMessage> getHistory() {
        return history;
    }

    public void setHistory(List<AiChatMessage> history) {
        this.history = (history != null) ? history : new ArrayList<>();
    }
}
