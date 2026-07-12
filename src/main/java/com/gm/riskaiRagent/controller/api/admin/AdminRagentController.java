package com.gm.riskaiRagent.controller.api.admin;

import com.gm.riskaiRagent.common.Result;
import com.gm.riskaiRagent.dto.PortalRagentRequest;
import com.gm.riskaiRagent.dto.RagentRequest;
import com.gm.riskaiRagent.dto.RagentResponse;
import com.gm.riskaiRagent.service.RagRagentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端问答测试接口，允许管理员指定分类范围验证 RAG 效果。
 */
@Tag(name = "AdminRagent")
@RestController
@RequestMapping("/api/admin/ragent")
@RequiredArgsConstructor
public class AdminRagentController {

    private final RagRagentService ragRagentService;

    @PostMapping("/ask")
    public Result<RagentResponse> ask(@Valid @RequestBody PortalRagentRequest request) {
        RagentRequest ragentRequest = new RagentRequest();
        ragentRequest.setQuestion(request.getQuestion());
        ragentRequest.setIncludeReferences(request.isIncludeReferences());
        return Result.success(ragRagentService.ask(ragentRequest, request.getCategoryIds()));
    }
}
