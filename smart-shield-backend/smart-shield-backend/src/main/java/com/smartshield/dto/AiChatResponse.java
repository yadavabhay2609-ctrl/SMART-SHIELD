package com.smartshield.dto;

import java.util.ArrayList;
import java.util.List;

public class AiChatResponse {

    private boolean success = true;
    private String answer;
    private String source; // "GEMINI", "LOCAL_KB", "EMERGENCY_OVERRIDE", "UNAVAILABLE"
    private String disclaimer = "Smart Shield AI Assistant provides general civic and legal information, not formal legal counsel. For life-threatening emergencies, call 112.";
    private boolean fallbackUsed = false;
    private List<String> suggestedActions = new ArrayList<>();

    public AiChatResponse() {
    }

    public AiChatResponse(String answer, String source, boolean fallbackUsed, List<String> suggestedActions) {
        this.success = true;
        this.answer = answer;
        this.source = source;
        this.fallbackUsed = fallbackUsed;
        this.suggestedActions = suggestedActions != null ? suggestedActions : new ArrayList<>();
    }

    public static AiChatResponse ofGemini(String answer, List<String> suggestedActions) {
        return new AiChatResponse(answer, "GEMINI", false, suggestedActions);
    }

    public static AiChatResponse ofLocalKb(String answer, List<String> suggestedActions) {
        return new AiChatResponse(answer, "LOCAL_KB", true, suggestedActions);
    }

    public static AiChatResponse ofEmergency(String answer) {
        AiChatResponse resp = new AiChatResponse(
                answer,
                "EMERGENCY_OVERRIDE",
                false,
                List.of("Call 112 Emergency", "Open Women Safety SOS", "Contact Local Police")
        );
        resp.setDisclaimer("URGENT: This appears to be an active safety emergency. Prioritize immediate physical safety.");
        return resp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public List<String> getSuggestedActions() {
        return suggestedActions;
    }

    public void setSuggestedActions(List<String> suggestedActions) {
        this.suggestedActions = (suggestedActions != null) ? suggestedActions : new ArrayList<>();
    }
}
