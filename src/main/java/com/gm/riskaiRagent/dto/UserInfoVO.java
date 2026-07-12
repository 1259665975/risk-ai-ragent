package com.gm.riskaiRagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息视图对象，隐藏密码等敏感字段后返回给前端。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String role;
    private Integer status;
    private String createTime;
}
