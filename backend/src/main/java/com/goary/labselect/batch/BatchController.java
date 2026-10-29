package com.goary.labselect.batch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.goary.labselect.auth.UserContext;
import com.goary.labselect.batch.entity.BatchTeacher;
import com.goary.labselect.batch.entity.CourseBatch;
import com.goary.labselect.batch.mapper.BatchTeacherMapper;
import com.goary.labselect.batch.mapper.CourseBatchMapper;
import com.goary.labselect.classroom.entity.Classroom;
import com.goary.labselect.classroom.mapper.ClassroomMapper;
import com.goary.labselect.common.BizException;
import com.goary.labselect.common.RequireRole;
import com.goary.labselect.common.Result;
import com.goary.labselect.course.entity.Course;
import com.goary.labselect.course.mapper.CourseMapper;
import com.goary.labselect.selection.entity.CourseSelection;
import com.goary.labselect.selection.mapper.CourseSelectionMapper;
import com.goary.labselect.user.entity.SysUser;
import com.goary.labselect.user.mapper.SysUserMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequireRole("teacher")
@RequiredArgsConstructor
public class BatchController {

    private final CourseMapper courseMapper;
    private final CourseBatchMapper batchMapper;
    private final BatchTeacherMapper batchTeacherMapper;
    private final ClassroomMapper classroomMapper;
    private final SysUserMapper sysUserMapper;
    private final CourseSelectionMapper selectionMapper;

    @GetMapping("/courses/{courseId}/batches")
    public Result<List<BatchVO>> listByCourse(@PathVariable Long courseId) {
        List<CourseBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<CourseBatch>()
                .eq(CourseBatch::getCourseId, courseId)
                .orderByAsc(CourseBatch::getId));
        return Result.ok(batches.stream().map(this::toVO).collect(Collectors.toList()));
    }

    @PostMapping("/batches")
    public Result<Void> create(@Valid @RequestBody BatchForm form) {
        Course course = mustOwnCourse(form.getCourseId());
        if (!"published".equals(course.getStatus())) {
            throw new BizException("请先发布课程再创建批次");
        }
        checkBatchNoUnique(form.getCourseId(), form.getBatchNo(), null);
        CourseBatch batch = new CourseBatch();
        applyAndCheck(batch, form);
        batch.setSelectedCount(0);
        batch.setSelectOpen(0);
        batch.setStatus("draft");
        batchMapper.insert(batch);
        saveTeachers(batch.getId(), form.getTeacherIds());
        return Result.ok();
    }

    @PutMapping("/batches/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody BatchForm form) {
        CourseBatch batch = mustOwnBatch(id);
        if (!"draft".equals(batch.getStatus())) {
            throw new BizException("仅草稿状态的批次可编辑");
        }
        checkBatchNoUnique(form.getCourseId(), form.getBatchNo(), id);
        applyAndCheck(batch, form);
        batchMapper.updateById(batch);
        saveTeachers(id, form.getTeacherIds());
        return Result.ok();
    }

    @PostMapping("/batches/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        CourseBatch batch = mustOwnBatch(id);
        if (!"draft".equals(batch.getStatus())) {
            throw new BizException("仅草稿状态的批次可发布");
        }
        batch.setStatus("published");
        batchMapper.updateById(batch);
        return Result.ok();
    }

    @PostMapping("/batches/{id}/open")
    public Result<Void> open(@PathVariable Long id) {
        CourseBatch batch = mustOwnBatch(id);
        if (!"published".equals(batch.getStatus())) {
            throw new BizException("请先发布批次再开放选课");
        }
        batch.setSelectOpen(1);
        batchMapper.updateById(batch);
        return Result.ok();
    }

    @PostMapping("/batches/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        CourseBatch batch = mustOwnBatch(id);
        batch.setSelectOpen(0);
        batch.setStatus("closed");
        batchMapper.updateById(batch);
        return Result.ok();
    }

    @DeleteMapping("/batches/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        CourseBatch batch = mustOwnBatch(id);
        if (!"draft".equals(batch.getStatus())) {
            throw new BizException("仅草稿状态的批次可删除");
        }
        batchMapper.deleteById(id);
        batchTeacherMapper.delete(new LambdaQueryWrapper<BatchTeacher>().eq(BatchTeacher::getBatchId, id));
        return Result.ok();
    }

    @GetMapping("/batches/{id}/students")
    public Result<List<Map<String, Object>>> students(@PathVariable Long id) {
        mustOwnBatch(id);
        List<CourseSelection> selections = selectionMapper.selectList(new LambdaQueryWrapper<CourseSelection>()
                .eq(CourseSelection::getBatchId, id)
                .eq(CourseSelection::getStatus, "selected")
                .orderByAsc(CourseSelection::getSelectTime));
        List<Long> studentIds = selections.stream().map(CourseSelection::getStudentId).collect(Collectors.toList());
        Map<Long, SysUser> userMap = studentIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(studentIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u));
        List<Map<String, Object>> rows = selections.stream().map(s -> {
            SysUser u = userMap.get(s.getStudentId());
            return Map.<String, Object>of(
                    "selectionId", s.getId(),
                    "username", u == null ? "" : u.getUsername(),
                    "realName", u == null ? "" : u.getRealName(),
                    "className", u == null || u.getClassName() == null ? "" : u.getClassName(),
                    "selectTime", s.getSelectTime().toString());
        }).collect(Collectors.toList());
        return Result.ok(rows);
    }

    private void applyAndCheck(CourseBatch batch, BatchForm form) {
        if (!form.getEndTime().isAfter(form.getStartTime())) {
            throw new BizException("结束时间必须晚于开始时间");
        }
        Classroom classroom = classroomMapper.selectById(form.getClassroomId());
        if (classroom == null || classroom.getStatus() != 1) {
            throw new BizException("教室不存在或已停用");
        }
        batch.setCourseId(form.getCourseId());
        batch.setBatchNo(form.getBatchNo());
        batch.setClassroomId(form.getClassroomId());
        batch.setStartTime(form.getStartTime());
        batch.setEndTime(form.getEndTime());
        // 容量固定取教室容量
        batch.setCapacity(classroom.getCapacity());
        // 教室时间冲突校验
        Long conflicts = batchMapper.selectCount(new LambdaQueryWrapper<CourseBatch>()
                .eq(CourseBatch::getClassroomId, form.getClassroomId())
                .ne(batch.getId() != null, CourseBatch::getId, batch.getId())
                .ne(CourseBatch::getStatus, "closed")
                .lt(CourseBatch::getStartTime, form.getEndTime())
                .gt(CourseBatch::getEndTime, form.getStartTime()));
        if (conflicts > 0) {
            throw new BizException("该教室在此时间段已有其他批次，请更换教室或时间");
        }
    }

    private void saveTeachers(Long batchId, List<Long> teacherIds) {
        batchTeacherMapper.delete(new LambdaQueryWrapper<BatchTeacher>().eq(BatchTeacher::getBatchId, batchId));
        for (Long teacherId : teacherIds.stream().distinct().collect(Collectors.toList())) {
            SysUser teacher = sysUserMapper.selectById(teacherId);
            if (teacher == null || !"teacher".equals(teacher.getRole())) {
                throw new BizException("授课教师不存在：" + teacherId);
            }
            BatchTeacher bt = new BatchTeacher();
            bt.setBatchId(batchId);
            bt.setTeacherId(teacherId);
            batchTeacherMapper.insert(bt);
        }
    }

    private void checkBatchNoUnique(Long courseId, String batchNo, Long excludeId) {
        Long count = batchMapper.selectCount(new LambdaQueryWrapper<CourseBatch>()
                .eq(CourseBatch::getCourseId, courseId)
                .eq(CourseBatch::getBatchNo, batchNo)
                .ne(excludeId != null, CourseBatch::getId, excludeId));
        if (count > 0) {
            throw new BizException("该课程下批次号已存在：" + batchNo);
        }
    }

    private Course mustOwnCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException("课程不存在");
        }
        if (!course.getCreatorId().equals(UserContext.getUserId())) {
            throw new BizException(403, "只能操作自己创建的课程");
        }
        return course;
    }

    private CourseBatch mustOwnBatch(Long batchId) {
        CourseBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException("批次不存在");
        }
        mustOwnCourse(batch.getCourseId());
        return batch;
    }

    private BatchVO toVO(CourseBatch batch) {
        BatchVO vo = new BatchVO();
        BeanUtils.copyProperties(batch, vo);
        Classroom classroom = classroomMapper.selectById(batch.getClassroomId());
        if (classroom != null) {
            vo.setClassroomName(classroom.getName());
            vo.setClassroomLocation(classroom.getLocation());
        }
        List<Long> teacherIds = batchTeacherMapper.selectList(
                        new LambdaQueryWrapper<BatchTeacher>().eq(BatchTeacher::getBatchId, batch.getId()))
                .stream().map(BatchTeacher::getTeacherId).collect(Collectors.toList());
        vo.setTeacherNames(teacherIds.isEmpty() ? List.of()
                : sysUserMapper.selectBatchIds(teacherIds).stream().map(SysUser::getRealName).collect(Collectors.toList()));
        vo.setRemaining(batch.getCapacity() - batch.getSelectedCount());
        return vo;
    }

    @Data
    public static class BatchForm {
        @NotNull(message = "课程不能为空")
        private Long courseId;
        @NotBlank(message = "批次号不能为空")
        private String batchNo;
        @NotNull(message = "教室不能为空")
        private Long classroomId;
        @NotEmpty(message = "授课教师不能为空")
        private List<Long> teacherIds;
        @NotNull(message = "开始时间不能为空")
        private LocalDateTime startTime;
        @NotNull(message = "结束时间不能为空")
        private LocalDateTime endTime;
    }
}
