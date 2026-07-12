package com.gm.riskaiRagent.controller.api.admin;

import com.gm.riskaiRagent.common.Result;
import com.gm.riskaiRagent.dto.UserInfoVO;
import com.gm.riskaiRagent.dto.UserSaveRequest;
import com.gm.riskaiRagent.service.SysUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端用户接口，提供用户列表、新增、编辑和删除能力。
 */
@Tag(name = "AdminUsers")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final SysUserService sysUserService;

    @GetMapping
    public Result<List<UserInfoVO>> list() {
        return Result.success(sysUserService.listAll());
    }

    @PostMapping
    public Result<UserInfoVO> create(@RequestBody UserSaveRequest request) {
        return Result.success(sysUserService.create(request));
    }

    @PutMapping("/{id}")
    public Result<UserInfoVO> update(@PathVariable Long id, @RequestBody UserSaveRequest request) {
        return Result.success(sysUserService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return Result.success();
    }
}
