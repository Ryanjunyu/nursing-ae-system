package com.his.ae.service;

import com.his.ae.vo.LoginVO;
import com.his.ae.vo.UserInfoVO;

import java.util.List;

public interface UserService {

    /** 登录，返回 JWT */
    String login(LoginVO vo);

    /** 当前登录用户信息 */
    UserInfoVO currentUser();

    /** 按数据范围查询用户列表（非管理员仅本科室） */
    List<UserInfoVO> listByScope();
}
