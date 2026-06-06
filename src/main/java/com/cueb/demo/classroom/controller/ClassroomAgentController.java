package com.cueb.demo.classroom.controller;

import com.cueb.demo.classroom.SystemPrompt;
import com.cueb.demo.classroom.context.ClassroomQueryContext;
import com.cueb.demo.classroom.service.ClassroomService;
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
    private final ClassroomService classroomService;
    private final GuestChatHistoryService historyService;

    public ClassroomAgentController(
            @Qualifier("chatClientV1") ChatClient chatClient,
            @Qualifier("chatClientV2") ChatClient chatClientV2,
            ClassroomQueryContext queryContext,
            ClassroomService classroomService,
            GuestChatHistoryService historyService) {
        this.chatClient = chatClient;
        this.chatClientV2 = chatClientV2;
        this.queryContext = queryContext;
        this.classroomService = classroomService;
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

        // ---- 预查询教室数据 ----
        List<ClassroomStatusVO> classrooms = classroomService.listAllStatus();
        String dataBlock = buildClassroomDataBlock(classrooms);

        // ---- 构建消息 ----
        // 数据块附在用户消息末尾，确保在历史之后、AI 最后看到
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SystemPrompt.V2_VALUE));
        if (useMemory) {
            messages.addAll(historyService.getHistory(token));
        }
        messages.add(new UserMessage(dataBlock + "\n\n用户问题：" + message));

        // ---- 调用 AI（V2 工具链，只含 reportOccupancy）----
        String reply = chatClientV2.prompt()
                .messages(messages)
                .call()
                .content();

        // ---- 记录历史（不记录注入的数据块，避免 Prompt 膨胀）----
        if (useMemory) {
            historyService.appendHistory(token, message, reply);
        }

        // ---- 构造响应 ----
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("reply", reply);

        if (includeVO) {
            if (queryContext.getLastReportResult() != null) {
                classrooms = classroomService.listAllStatus();
            }
            // 只返回 AI 回复中提到的教室，前端按此展示
            classrooms = filterByMentioned(classrooms, reply);
            body.put("classrooms", classrooms);
        }

        return ResponseEntity.ok()
                .header("X-Guest-Token", token)
                .body(body);
    }

    /** 将教室数据格式化为 AI 可直接使用的上下文文本 */
    private String buildClassroomDataBlock(List<ClassroomStatusVO> classrooms) {
        StringBuilder sb = new StringBuilder("【当前教室实时数据 —— 务必基于此数据回答，禁止编造】\n");
        for (ClassroomStatusVO v : classrooms) {
            sb.append(v.getName())
              .append(" | ").append(v.getStatus())
              .append(" | 容量").append(v.getCapacity());
            if (v.getCourseName() != null) {
                sb.append(" | ").append(v.getCourseName())
                  .append(" | ").append(v.getTeacherName());
            }
            if (v.getPeopleCount() != null) {
                sb.append(" | 人数").append(v.getPeopleCount());
            }
            if (v.getRemainingMinutes() != null && v.getRemainingMinutes() > 0) {
                sb.append(" | 剩余").append(formatMinutes(v.getRemainingMinutes()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /** 按 AI 回复文本过滤：只保留回复中出现了名称的教室 */
    private List<ClassroomStatusVO> filterByMentioned(List<ClassroomStatusVO> all, String reply) {
        if (reply == null || reply.isBlank()) return List.of();
        return all.stream()
                .filter(v -> reply.contains(v.getName()))
                .toList();
    }

    private static String formatMinutes(long totalMinutes) {
        if (totalMinutes <= 0) return "0分钟";
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours > 0) return hours + "小时" + (minutes > 0 ? minutes + "分钟" : "");
        return minutes + "分钟";
    }
}
