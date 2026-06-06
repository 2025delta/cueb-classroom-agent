package com.cueb.demo.classroom.controller;

import com.cueb.demo.classroom.SystemPrompt;
import com.cueb.demo.classroom.context.ClassroomQueryContext;
import com.cueb.demo.classroom.service.GuestChatHistoryService;
import com.cueb.demo.classroom.vo.ClassroomStatusVO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class ClassroomAgentController {

    private final ChatClient chatClient;
    private final ChatClient chatClientV2;
    private final ClassroomQueryContext queryContext;
    private final GuestChatHistoryService historyService;

    public ClassroomAgentController(
            @Qualifier("chatClientV1") ChatClient chatClient,
            @Qualifier("chatClientV2") ChatClient chatClientV2,
            ClassroomQueryContext queryContext,
            GuestChatHistoryService historyService) {
        this.chatClient = chatClient;
        this.chatClientV2 = chatClientV2;
        this.queryContext = queryContext;
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

    // ──────────────────────────────────────────────
    // V2 统一接口 — 支持可开关记忆 + 可选返回 VO
    // ──────────────────────────────────────────────

    /** 请求体 DTO */
    public record ChatRequest(
            String message,
            Boolean memory,
            Boolean includeVO) {
    }

    /**
     * 统一 AI 对话接口（V2）。
     * - memory=true  使用 Redis 多轮记忆
     * - includeVO=true  返回结构化教室状态列表 {@link ClassroomStatusVO}
     */
    @PostMapping("/chat/v2")
    public ResponseEntity<Map<String, Object>> chatV2(
            @RequestBody ChatRequest req,
            @RequestHeader(value = "X-Guest-Token", required = false) String token) {

        String message = req.message();
        boolean useMemory = req.memory() != null && req.memory();
        boolean includeVO = req.includeVO() != null && req.includeVO();

        // ---- token ----
        if (token == null || token.isBlank()) {
            token = historyService.generateToken();
        }

        // ---- 构建消息 ----
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SystemPrompt.VALUE));
        if (useMemory) {
            messages.addAll(historyService.getHistory(token));
        }
        messages.add(new UserMessage(message));

        // ---- 调用 AI（V2 工具链，自动写入 queryContext）----
        String reply = chatClientV2.prompt()
                .messages(messages)
                .call()
                .content();

        // ---- 记录历史 ----
        if (useMemory) {
            historyService.appendHistory(token, message, reply);
        }

        // ---- 构造响应 ----
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("reply", reply);

        if (includeVO) {
            List<ClassroomStatusVO> classrooms = queryContext.getLastQueryResult();
            body.put("classrooms", classrooms != null ? classrooms : List.of());
        }

        return ResponseEntity.ok()
                .header("X-Guest-Token", token)
                .body(body);
    }
}
