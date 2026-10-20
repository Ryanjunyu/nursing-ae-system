package com.his.ae.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.his.ae.common.BizException;
import com.his.ae.entity.SysDepartment;
import com.his.ae.entity.SysUser;
import com.his.ae.mapper.SysDepartmentMapper;
import com.his.ae.mapper.SysUserMapper;
import com.his.ae.security.CurrentUserHolder;
import com.his.ae.security.JwtUtil;
import com.his.ae.security.LoginUser;
import com.his.ae.service.UserService;
import com.his.ae.vo.LoginVO;
import com.his.ae.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final SysDepartmentMapper deptMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    @com.his.ae.annotation.Audit(actionType = "LOGIN", targetType = "用户")
    public String login(LoginVO vo) {
        SysUser u = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", vo.getUsername()));
        if (u == null || u.getStatus() == 0) {
            throw new BizException(401, "用户不存在或已停用");
        }
        if (!passwordEncoder.matches(vo.getPassword(), u.getPassword())) {
            throw new BizException(401, "密码错误");
        }
        LoginUser lu = new LoginUser();
        lu.setUserId(u.getUserId());
        lu.setUsername(u.getUsername());
        lu.setRealName(u.getRealName());
        lu.setRole(u.getRole());
        lu.setDeptId(u.getDeptId());
        // 登录请求本身不带令牌，拦截器不会注入当前用户上下文；
        // 这里手动写入，保证同一次请求内 currentUser() 能取到登录用户（请求结束由拦截器清理）。
        CurrentUserHolder.set(lu);
        return jwtUtil.generate(lu);
    }

    @Override
    public UserInfoVO currentUser() {
        LoginUser lu = CurrentUserHolder.get();
        if (lu == null) {
            throw new BizException(401, "未登录");
        }
        return toVO(userMapper.selectById(lu.getUserId()));
    }

    @Override
    public List<UserInfoVO> listByScope() {
        LoginUser lu = CurrentUserHolder.get();
        if (lu == null) {
            throw new BizException(401, "未登录");
        }
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        // 数据范围隔离：非管理员仅可看本科室用户
        if (!"ADMIN".equals(lu.getRole()) && lu.getDeptId() != null) {
            qw.eq("dept_id", lu.getDeptId());
        }
        qw.orderByAsc("user_id");
        return userMapper.selectList(qw).stream().map(this::toVO).toList();
    }

    private UserInfoVO toVO(SysUser u) {
        if (u == null) {
            return null;
        }
        UserInfoVO v = new UserInfoVO();
        v.setUserId(u.getUserId());
        v.setUsername(u.getUsername());
        v.setRealName(u.getRealName());
        v.setRole(u.getRole());
        v.setDeptId(u.getDeptId());
        if (u.getDeptId() != null) {
            SysDepartment d = deptMapper.selectById(u.getDeptId());
            if (d != null) {
                v.setDeptName(d.getDeptName());
            }
        }
        return v;
    }
}
