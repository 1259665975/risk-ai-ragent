package com.gm.riskaiRagent.service;

import com.gm.riskaiRagent.common.BusinessException;
import com.gm.riskaiRagent.common.ResultCode;
import com.gm.riskaiRagent.dto.PasswordUpdateRequest;
import com.gm.riskaiRagent.dto.ProfileUpdateRequest;
import com.gm.riskaiRagent.dto.UserInfoVO;
import com.gm.riskaiRagent.entity.SysUser;
import com.gm.riskaiRagent.mapper.SysUserMapper;
import com.gm.riskaiRagent.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户资料服务，负责当前用户资料查询、资料更新和密码修改。
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final SysUserMapper sysUserMapper;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public UserInfoVO getProfile() {
        return authService.toUserInfo(requireCurrentUser());
    }

    public UserInfoVO updateProfile(ProfileUpdateRequest request) {
        SysUser user = requireCurrentUser();
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        sysUserMapper.updateById(user);
        return authService.toUserInfo(user);
    }

    public void updatePassword(PasswordUpdateRequest request) {
        if (!StringUtils.hasText(request.getOldPassword()) || !StringUtils.hasText(request.getNewPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "密码不能为空");
        }
        SysUser user = requireCurrentUser();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);
    }

    private SysUser requireCurrentUser() {
        Long userId = AuthContext.userId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return user;
    }
}
