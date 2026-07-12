package com.gm.riskaiRagent.config;

import com.gm.riskaiRagent.entity.SysCategory;
import com.gm.riskaiRagent.entity.SysUser;
import com.gm.riskaiRagent.mapper.SysCategoryMapper;
import com.gm.riskaiRagent.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 系统启动数据初始化器，在空库时创建默认用户、分类等基础数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final SysUserMapper sysUserMapper;
    private final SysCategoryMapper sysCategoryMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        initUsers();
        initCategories();
    }

    private void initUsers() {
        createUserIfAbsent("admin", "管理员", "ADMIN");
        createUserIfAbsent("user1", "普通用户", "USER");
    }

    private void createUserIfAbsent(String username, String nickname, String role) {
        Long count = sysUserMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username));
        if (count != null && count > 0) {
            return;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("123456"));
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus(1);
        sysUserMapper.insert(user);
        log.info("Initialized default user: {}", username);
    }

    private void initCategories() {
        createCategoryIfAbsent("风控政策", "企业内部风控政策与制度");
        createCategoryIfAbsent("合规制度", "合规审查与监管要求");
        createCategoryIfAbsent("操作手册", "业务操作与流程规范");
    }

    private void createCategoryIfAbsent(String name, String description) {
        Long count = sysCategoryMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysCategory>()
                        .eq(SysCategory::getName, name));
        if (count != null && count > 0) {
            return;
        }
        SysCategory category = new SysCategory();
        category.setName(name);
        category.setDescription(description);
        sysCategoryMapper.insert(category);
        log.info("Initialized default category: {}", name);
    }
}
