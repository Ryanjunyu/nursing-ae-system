package com.his.ae.service;

import com.his.ae.vo.LoginVO;
import com.his.ae.vo.UserInfoVO;
import com.his.ae.vo.UserSaveVO;

import java.util.List;

public interface UserService {

    /** 登录，返回 JWT */
    String login(LoginVO vo);

    /** 当前登录用户信息 */
    UserInfoVO currentUser();

    /** 按数据范围查询用户列表（非管理员仅本科室） */
    List<UserInfoVO> listByScope();

    /** 新增用户（仅管理员），返回 userId */
    Integer createUser(UserSaveVO vo);

    /** 修改用户（仅管理员）：可改姓名/角色/科室/状态，密码非空则重置 */
    void updateUser(Integer userId, UserSaveVO vo);
}
