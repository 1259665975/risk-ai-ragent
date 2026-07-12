package com.gm.riskaiRagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gm.riskaiRagent.common.BusinessException;
import com.gm.riskaiRagent.common.ResultCode;
import com.gm.riskaiRagent.dto.UserInfoVO;
import com.gm.riskaiRagent.dto.UserSaveRequest;
import com.gm.riskaiRagent.entity.SysUser;
import com.gm.riskaiRagent.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统用户服务，负责用户管理、密码加密和用户信息转换。
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public List<UserInfoVO> listAll() {
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .orderByDesc(SysUser::getId))
                .stream()
                .map(authService::toUserInfo)
                .collect(Collectors.toList());
    }

    public UserInfoVO create(UserSaveRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名和密码不能为空");
        }
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setRole(StringUtils.hasText(request.getRole()) ? request.getRole().toUpperCase() : "USER");
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        sysUserMapper.insert(user);
        return authService.toUserInfo(user);
    }

    public UserInfoVO update(Long id, UserSaveRequest request) {
        SysUser user = requireUser(id);
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (StringUtils.hasText(request.getRole())) {
            user.setRole(request.getRole().toUpperCase());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        sysUserMapper.updateById(user);
        return authService.toUserInfo(user);
    }

    public void delete(Long id) {
        requireUser(id);
        sysUserMapper.deleteById(id);
    }

    public SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return user;
    }

    public SysUser getByUsername(String username) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
    }
}
