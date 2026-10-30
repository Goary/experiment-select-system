package com.goary.labselect.course;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.goary.labselect.auth.UserContext;
import com.goary.labselect.common.BizException;
import com.goary.labselect.common.RequireRole;
import com.goary.labselect.common.Result;
import com.goary.labselect.course.entity.Course;
import com.goary.labselect.course.mapper.CourseMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

import java.math.BigDecimal;

@Tag(name = "教师-课程管理", description = "仅 teacher")
@RestController
@RequestMapping("/api/courses")
@RequireRole("teacher")
@RequiredArgsConstructor
public class CourseController {

    private final CourseMapper courseMapper;

    @Operation(summary = "我创建的课程分页")
    @GetMapping("/my")
    public Result<Page<Course>> my(@RequestParam(defaultValue = "1") long page,
                                   @RequestParam(defaultValue = "10") long size,
                                   @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Course> qw = new LambdaQueryWrapper<Course>()
                .eq(Course::getCreatorId, UserContext.getUserId())
                .like(StringUtils.hasText(keyword), Course::getName, keyword)
                .orderByDesc(Course::getId);
        return Result.ok(courseMapper.selectPage(new Page<>(page, size), qw));
    }

    @Operation(summary = "课程详情")
    @GetMapping("/{id}")
    public Result<Course> detail(@PathVariable Long id) {
        return Result.ok(mustOwn(id));
    }

    @Operation(summary = "创建课程")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CourseForm form) {
        checkCodeUnique(form.getCourseCode(), null);
        Course course = new Course();
        apply(course, form);
        course.setCreatorId(UserContext.getUserId());
        course.setStatus("draft");
        courseMapper.insert(course);
        return Result.ok();
    }

    @Operation(summary = "编辑课程（仅草稿）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CourseForm form) {
        Course course = mustOwn(id);
        if ("published".equals(course.getStatus())) {
            throw new BizException("课程已发布，不能再编辑基本信息");
        }
        checkCodeUnique(form.getCourseCode(), id);
        apply(course, form);
        courseMapper.updateById(course);
        return Result.ok();
    }

    @Operation(summary = "发布课程")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        Course course = mustOwn(id);
        if (!"draft".equals(course.getStatus())) {
            throw new BizException("仅草稿状态的课程可发布");
        }
        course.setStatus("published");
        courseMapper.updateById(course);
        return Result.ok();
    }

    @Operation(summary = "删除课程（仅草稿）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Course course = mustOwn(id);
        if (!"draft".equals(course.getStatus())) {
            throw new BizException("仅草稿状态的课程可删除");
        }
        courseMapper.deleteById(id);
        return Result.ok();
    }

    private Course mustOwn(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BizException("课程不存在");
        }
        if (!course.getCreatorId().equals(UserContext.getUserId())) {
            throw new BizException(403, "只能操作自己创建的课程");
        }
        return course;
    }

    private void checkCodeUnique(String courseCode, Long excludeId) {
        Long count = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .eq(Course::getCourseCode, courseCode)
                .ne(excludeId != null, Course::getId, excludeId));
        if (count > 0) {
            throw new BizException("课程编号已存在：" + courseCode);
        }
    }

    private void apply(Course course, CourseForm form) {
        course.setCourseCode(form.getCourseCode());
        course.setName(form.getName());
        course.setDescription(form.getDescription());
        course.setCredit(form.getCredit());
    }

    @Data
    public static class CourseForm {
        @NotBlank(message = "课程编号不能为空")
        private String courseCode;
        @NotBlank(message = "课程名称不能为空")
        private String name;
        private String description;
        private BigDecimal credit;
    }
}
