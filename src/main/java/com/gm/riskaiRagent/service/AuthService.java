package com.gm.riskaiRagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gm.riskaiRagent.common.BusinessException;
import com.gm.riskaiRagent.common.ResultCode;
import com.gm.riskaiRagent.dto.LoginRequest;
import com.gm.riskaiRagent.dto.LoginResponse;
import com.gm.riskaiRagent.dto.UserInfoVO;
import com.gm.riskaiRagent.entity.SysUser;
import com.gm.riskaiRagent.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * 认证服务，负责账号密码校验、Token 生成和用户信息转换。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }

        String token = tokenService.createToken(user);
        return LoginResponse.builder()
                .token(token)
                .user(toUserInfo(user))
                .build();
    }

    public UserInfoVO toUserInfo(SysUser user) {
        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createTime(user.getCreatedAt() == null ? null : user.getCreatedAt().format(FMT))
                .build();
    }
}
