package com.his.ae.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.his.ae.entity.SysUser;
import com.his.ae.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 教学演示数据初始化：库内无用户时，插入三角色示例账号（密码统一 123456，BCrypt 加密）。
 * 真实课程演示请引导在页面注册/由管理员创建，此处仅为快速跑通原型。
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userMapper.selectCount(new QueryWrapper<>()) > 0) {
            return;
        }
        insert("admin", "系统管理员", "ADMIN", 5);      // 护理部（管理）
        insert("head1", "王护士长", "HEAD_NURSE", 1);    // 内科一病区
        insert("nurse1", "李护士", "NURSE", 1);          // 内科一病区
        insert("nurse2", "赵护士", "NURSE", 2);          // 内科二病区
    }

    private void insert(String username, String realName, String role, Integer deptId) {
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("123456"));
        u.setRealName(realName);
        u.setRole(role);
        u.setDeptId(deptId);
        u.setStatus(1);
        userMapper.insert(u);
    }
}
