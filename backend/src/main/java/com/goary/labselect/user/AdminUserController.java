package com.goary.labselect.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.goary.labselect.common.BizException;
import com.goary.labselect.common.RequireRole;
import com.goary.labselect.common.Result;
import com.goary.labselect.user.entity.SysUser;
import com.goary.labselect.user.mapper.SysUserMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@RequireRole("admin")
@RequiredArgsConstructor
public class AdminUserController {

    private static final Set<String> ROLES = Set.of("admin", "teacher", "student");
    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public Result<Page<SysUser>> page(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String role,
                                      @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>()
                .eq(StringUtils.hasText(role), SysUser::getRole, role)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysUser::getEmployeeNo, keyword)
                        .or().like(SysUser::getUsername, keyword))
                .orderByDesc(SysUser::getId);
        return Result.ok(sysUserMapper.selectPage(new Page<>(page, size), qw));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserForm form) {
        checkRole(form.getRole());
        checkEmployeeNoUnique(form.getEmployeeNo(), null);
        SysUser user = new SysUser();
        apply(user, form);
        user.setPassword(passwordEncoder.encode(
                StringUtils.hasText(form.getPassword()) ? form.getPassword() : DEFAULT_PASSWORD));
        sysUserMapper.insert(user);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserForm form) {
        SysUser user = mustExist(id);
        checkRole(form.getRole());
        checkEmployeeNoUnique(form.getEmployeeNo(), id);
        apply(user, form);
        sysUserMapper.updateById(user);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SysUser user = mustExist(id);
        if ("admin".equals(user.getRole())) {
            throw new BizException("不能删除管理员账号");
        }
        sysUserMapper.deleteById(id);
        return Result.ok();
    }

    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        SysUser user = mustExist(id);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        sysUserMapper.updateById(user);
        return Result.ok();
    }

    private SysUser mustExist(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }

    private void checkRole(String role) {
        if (!ROLES.contains(role)) {
            throw new BizException("非法角色：" + role);
        }
    }

    private void checkEmployeeNoUnique(String employeeNo, Long excludeId) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmployeeNo, employeeNo)
                .ne(excludeId != null, SysUser::getId, excludeId));
        if (count > 0) {
            throw new BizException("工号/学号已存在：" + employeeNo);
        }
    }

    private void apply(SysUser user, UserForm form) {
        user.setEmployeeNo(form.getEmployeeNo());
        user.setUsername(form.getUsername());
        user.setRole(form.getRole());
        user.setClassName(form.getClassName());
        user.setStatus(form.getStatus() == null ? 1 : form.getStatus());
    }

    @Data
    public static class UserForm {
        @NotBlank(message = "工号/学号不能为空")
        private String employeeNo;
        @NotBlank(message = "姓名不能为空")
        private String username;
        @NotBlank(message = "角色不能为空")
        private String role;
        private String className;
        private Integer status;
        private String password;
    }
}
