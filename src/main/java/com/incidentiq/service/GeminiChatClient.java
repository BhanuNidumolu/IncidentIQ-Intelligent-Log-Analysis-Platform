package com.incidentiq.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeminiChatClient {

    private final ChatClient chatClient;

    public String chat(String prompt) {
        try {
            return chatClient
                    .prompt(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            throw new RuntimeException("AI Chat Error: " + e.getMessage(), e);
        }
    }
}
