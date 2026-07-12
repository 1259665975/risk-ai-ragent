package com.gm.riskaiRagent.dto;

import lombok.Data;

/**
 * 密码修改请求体，承载原密码和新密码。
 */
@Data
public class PasswordUpdateRequest {

    private String oldPassword;
    private String newPassword;
}
