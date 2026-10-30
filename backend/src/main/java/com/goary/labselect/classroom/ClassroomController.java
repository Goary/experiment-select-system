package com.goary.labselect.classroom;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.goary.labselect.classroom.entity.Classroom;
import com.goary.labselect.classroom.mapper.ClassroomMapper;
import com.goary.labselect.common.BizException;
import com.goary.labselect.common.RequireRole;
import com.goary.labselect.common.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@Tag(name = "超管-教室管理", description = "仅 admin")
@RestController
@RequestMapping("/api/admin/classrooms")
@RequireRole("admin")
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomMapper classroomMapper;

    @Operation(summary = "教室列表")
    @GetMapping
    public Result<List<Classroom>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(classroomMapper.selectList(new LambdaQueryWrapper<Classroom>()
                .like(StringUtils.hasText(keyword), Classroom::getName, keyword)
                .orderByAsc(Classroom::getId)));
    }

    @Operation(summary = "新增教室")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ClassroomForm form) {
        Classroom classroom = new Classroom();
        apply(classroom, form);
        classroomMapper.insert(classroom);
        return Result.ok();
    }

    @Operation(summary = "编辑教室")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ClassroomForm form) {
        Classroom classroom = classroomMapper.selectById(id);
        if (classroom == null) {
            throw new BizException("教室不存在");
        }
        apply(classroom, form);
        classroomMapper.updateById(classroom);
        return Result.ok();
    }

    @Operation(summary = "删除教室")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        classroomMapper.deleteById(id);
        return Result.ok();
    }

    private void apply(Classroom classroom, ClassroomForm form) {
        classroom.setName(form.getName());
        classroom.setLocation(form.getLocation());
        classroom.setCapacity(form.getCapacity());
        classroom.setStatus(form.getStatus() == null ? 1 : form.getStatus());
    }

    @Data
    public static class ClassroomForm {
        @NotBlank(message = "教室名称不能为空")
        private String name;
        private String location;
        @NotNull(message = "容量不能为空")
        private Integer capacity;
        private Integer status;
    }
}
