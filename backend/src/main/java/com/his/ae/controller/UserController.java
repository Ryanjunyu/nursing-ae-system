package com.his.ae.controller;

import com.his.ae.annotation.Audit;
import com.his.ae.common.Result;
import com.his.ae.service.UserService;
import com.his.ae.vo.UserInfoVO;
import com.his.ae.vo.UserSaveVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 用户列表（管理员全部、护士长本科室） */
    @GetMapping
    @Audit(actionType = "VIEW", targetType = "用户")
    public Result<List<UserInfoVO>> list() {
        return Result.ok(userService.listByScope());
    }

    /** 新增用户（仅管理员） */
    @PostMapping
    @Audit(actionType = "CREATE", targetType = "用户")
    public Result<Integer> create(@Valid @RequestBody UserSaveVO vo) {
        return Result.ok(userService.createUser(vo));
    }

    /** 修改用户（仅管理员） */
    @PutMapping("/{id}")
    @Audit(actionType = "UPDATE", targetType = "用户")
    public Result<Void> update(@PathVariable Integer id, @Valid @RequestBody UserSaveVO vo) {
        userService.updateUser(id, vo);
        return Result.ok();
    }
}
