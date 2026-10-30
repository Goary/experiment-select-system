package com.goary.labselect.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.goary.labselect.classroom.entity.Classroom;
import com.goary.labselect.classroom.mapper.ClassroomMapper;
import com.goary.labselect.user.entity.SysUser;
import com.goary.labselect.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "公共数据", description = "所有登录用户可用")
@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class CommonController {

    private final SysUserMapper sysUserMapper;
    private final ClassroomMapper classroomMapper;

    @Operation(summary = "教师下拉列表")
    @GetMapping("/teachers")
    public Result<List<Map<String, Object>>> teachers() {
        List<SysUser> teachers = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "teacher")
                .eq(SysUser::getStatus, 1)
                .orderByAsc(SysUser::getEmployeeNo));
        List<Map<String, Object>> list = teachers.stream()
                .map(t -> Map.<String, Object>of("id", t.getId(), "employeeNo", t.getEmployeeNo(), "username", t.getUsername()))
                .toList();
        return Result.ok(list);
    }

    @Operation(summary = "可用教室下拉列表")
    @GetMapping("/classrooms")
    public Result<List<Classroom>> classrooms() {
        return Result.ok(classroomMapper.selectList(new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getStatus, 1)
                .orderByAsc(Classroom::getName)));
    }
}
