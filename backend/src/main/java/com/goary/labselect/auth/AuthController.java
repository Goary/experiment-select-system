package com.goary.labselect.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.goary.labselect.common.BizException;
import com.goary.labselect.common.Result;
import com.goary.labselect.user.entity.SysUser;
import com.goary.labselect.user.mapper.SysUserMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证", description = "登录与当前用户")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Operation(summary = "登录，返回 JWT token 与用户信息")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmployeeNo, req.getEmployeeNo()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException("工号/学号或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BizException("账号已停用");
        }
        String token = jwtUtil.createToken(user.getId(), user.getRole());
        return Result.ok(new LoginResponse(token, user));
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<LoginUser> me() {
        LoginUser login = UserContext.get();
        SysUser user = sysUserMapper.selectById(login.getId());
        return Result.ok(new LoginUser(user.getId(), user.getEmployeeNo(), user.getUsername(), user.getRole()));
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "工号/学号不能为空")
        private String employeeNo;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class LoginResponse {
        private final String token;
        private final SysUser user;
    }
}
