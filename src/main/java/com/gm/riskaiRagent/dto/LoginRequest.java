package com.gm.riskaiRagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求体，承载用户名和密码。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "username 不能为空")
    private String username;

    @NotBlank(message = "password 不能为空")
    private String password;
}
