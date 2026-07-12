package com.gm.riskaiRagent.controller.api.user;

import com.gm.riskaiRagent.common.Result;
import com.gm.riskaiRagent.dto.PasswordUpdateRequest;
import com.gm.riskaiRagent.dto.ProfileUpdateRequest;
import com.gm.riskaiRagent.dto.UserInfoVO;
import com.gm.riskaiRagent.service.UserProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端个人中心接口，提供资料查询、资料更新和密码修改。
 */
@Tag(name = "UserProfile")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    public Result<UserInfoVO> profile() {
        return Result.success(userProfileService.getProfile());
    }

    @PutMapping("/profile")
    public Result<UserInfoVO> updateProfile(@RequestBody ProfileUpdateRequest request) {
        return Result.success(userProfileService.updateProfile(request));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestBody PasswordUpdateRequest request) {
        userProfileService.updatePassword(request);
        return Result.success();
    }
}
