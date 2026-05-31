package com.cueb.demo.classroom.controller;

import com.cueb.demo.classroom.SystemPrompt;
import com.cueb.demo.classroom.service.ClassroomAgentService;
import com.cueb.demo.classroom.service.GuestChatHistoryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class ClassroomAgentController {

    private final ChatClient chatClient;
    private final GuestChatHistoryService historyService;

    public ClassroomAgentController(ChatClient.Builder chatClientBuilder,
                                     ClassroomAgentService agentService,
                                     GuestChatHistoryService historyService) {
        this.chatClient = chatClientBuilder
                .defaultTools(agentService)
                .build();
        this.historyService = historyService;
    }

    @GetMapping("/guest-token")
    public Map<String, String> getGuestToken() {
        String token = historyService.generateToken();
        return Map.of("token", token);
    }

    @PostMapping("/chat")
    public String chat(@RequestBody String message) {
        return chatClient.prompt()
                .system(SystemPrompt.VALUE)
                .user(u -> u.text(message))
                .call()
                .content();
    }

    @PostMapping("/chat/memory")
    public ResponseEntity<Map<String, String>> chatWithMemory(
            @RequestBody String message,
            @RequestHeader(value = "X-Guest-Token", required = false) String token) {

        if (token == null || token.isBlank()) {
            token = historyService.generateToken();
        }

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SystemPrompt.VALUE));
        messages.addAll(historyService.getHistory(token));
        messages.add(new UserMessage(message));

        String reply = chatClient.prompt()
                .messages(messages)
                .call()
                .content();

        historyService.appendHistory(token, message, reply);

        return ResponseEntity.ok()
                .header("X-Guest-Token", token)
                .body(Map.of("token", token, "reply", reply));
    }
}
