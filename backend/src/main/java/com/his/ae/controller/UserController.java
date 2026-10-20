package com.his.ae.controller;

import com.his.ae.annotation.Audit;
import com.his.ae.common.Result;
import com.his.ae.service.UserService;
import com.his.ae.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @Audit(actionType = "VIEW", targetType = "用户")
    public Result<List<UserInfoVO>> list() {
        return Result.ok(userService.listByScope());
    }
}
