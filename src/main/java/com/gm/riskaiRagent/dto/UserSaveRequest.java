package com.gm.riskaiRagent.dto;

import lombok.Data;

/**
 * 用户保存请求体，承载管理员新增或编辑用户时的字段。
 */
@Data
public class UserSaveRequest {

    private String username;
    private String password;
    private String nickname;
    private String role;
    private Integer status;
}
