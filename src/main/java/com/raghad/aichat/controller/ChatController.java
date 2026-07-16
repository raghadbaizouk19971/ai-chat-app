package com.raghad.aichat.controller;

import com.raghad.aichat.dto.ChatRequest;
import com.raghad.aichat.dto.ChatResponse;
import com.raghad.aichat.service.OpenAiChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final OpenAiChatService chatService;

    public ChatController(OpenAiChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/api/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String reply = chatService.getChatReply(request.prompt(), request.history());
        return new ChatResponse(reply);
    }
}
