package com.gm.riskaiRagent.dto;

import lombok.Data;

/**
 * 个人资料更新请求体，承载昵称等可编辑用户信息。
 */
@Data
public class ProfileUpdateRequest {

    private String nickname;
    private String email;
}
