package com.his.ae.controller;

import com.his.ae.common.Result;
import com.his.ae.service.UserService;
import com.his.ae.vo.LoginVO;
import com.his.ae.vo.UserInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginVO vo) {
        String token = userService.login(vo);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", userService.currentUser());
        return Result.ok(data);
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.ok(userService.currentUser());
    }
}
