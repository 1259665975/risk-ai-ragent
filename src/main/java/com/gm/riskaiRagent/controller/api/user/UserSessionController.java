package com.gm.riskaiRagent.controller.api.user;

import com.gm.riskaiRagent.common.Result;
import com.gm.riskaiRagent.dto.SessionCreateRequest;
import com.gm.riskaiRagent.entity.ChatSession;
import com.gm.riskaiRagent.service.ChatSessionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端会话接口，管理当前用户的聊天会话和历史消息。
 */
@Tag(name = "UserSessions")
@RestController
@RequestMapping("/api/user/sessions")
@RequiredArgsConstructor
public class UserSessionController {

    private final ChatSessionService chatSessionService;

    @GetMapping
    public Result<List<ChatSession>> list() {
        return Result.success(chatSessionService.listSessions());
    }

    @PostMapping
    public Result<ChatSession> create(@RequestBody(required = false) SessionCreateRequest request) {
        String title = request == null ? null : request.getTitle();
        return Result.success(chatSessionService.createSession(title));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        chatSessionService.deleteSession(id);
        return Result.success();
    }

    @GetMapping("/{id}/messages")
    public Result<List<ChatSessionService.ChatMessageVO>> messages(@PathVariable Long id) {
        return Result.success(chatSessionService.listMessages(id));
    }
}
