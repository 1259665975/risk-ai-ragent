package com.gm.riskaiRagent.security;

import com.gm.riskaiRagent.entity.SysUser;
import lombok.Getter;
import lombok.Setter;

/**
 * 认证用户快照，只保存鉴权和角色判断所需的安全字段。
 */
@Getter
@Setter
public class AuthUser {

    private Long id;
    private String username;
    private String role;

    public static AuthUser from(SysUser user) {
        AuthUser authUser = new AuthUser();
        authUser.setId(user.getId());
        authUser.setUsername(user.getUsername());
        authUser.setRole(user.getRole());
        return authUser;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
