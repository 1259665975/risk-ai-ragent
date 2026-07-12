package com.gm.riskaiRagent.controller.api;

import com.gm.riskaiRagent.common.Result;
import com.gm.riskaiRagent.dto.LoginRequest;
import com.gm.riskaiRagent.dto.LoginResponse;
import com.gm.riskaiRagent.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口，处理账号密码登录并返回后续访问所需的 Token。
 */
@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }
}
