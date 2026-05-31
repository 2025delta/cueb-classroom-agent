package com.cueb.demo.classroom.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GuestChatHistoryService {

    private static final String KEY_PREFIX = "chat:guest:";
    private static final int TTL_MINUTES = 30;
    private static final int MAX_ROUNDS = 6;
    private static final int MAX_MESSAGES = MAX_ROUNDS * 2;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GuestChatHistoryService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 取出该游客的对话历史（不含 system prompt） */
    public List<Message> getHistory(String token) {
        String json = redis.opsForValue().get(KEY_PREFIX + token);
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, String>> raw = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, String>>>() {});
            List<Message> messages = new ArrayList<>();
            for (Map<String, String> entry : raw) {
                String role = entry.get("role");
                String content = entry.get("content");
                if ("user".equals(role)) {
                    messages.add(new UserMessage(content));
                } else if ("assistant".equals(role)) {
                    messages.add(new AssistantMessage(content));
                }
            }
            return messages;
        } catch (Exception e) {
            return List.of();
        }
    }

    /** 追加一轮对话并写回 Redis，超出上限则裁剪旧记录 */
    public void appendHistory(String token, String userMessage, String assistantReply) {
        String key = KEY_PREFIX + token;
        List<Map<String, String>> list;
        try {
            String json = redis.opsForValue().get(key);
            if (json != null && !json.isBlank()) {
                list = objectMapper.readValue(json,
                        new TypeReference<List<Map<String, String>>>() {});
            } else {
                list = new ArrayList<>();
            }
        } catch (Exception e) {
            list = new ArrayList<>();
        }

        list.add(Map.of("role", "user", "content", userMessage));
        list.add(Map.of("role", "assistant", "content", assistantReply));

        // 裁剪：只保留最近 MAX_MESSAGES 条
        if (list.size() > MAX_MESSAGES) {
            list = list.subList(list.size() - MAX_MESSAGES, list.size());
        }

        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(list),
                    Duration.ofMinutes(TTL_MINUTES));
        } catch (Exception ignored) {
        }
    }
}
