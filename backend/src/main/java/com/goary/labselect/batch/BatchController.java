package com.goary.labselect.batch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.goary.labselect.auth.UserContext;
import com.goary.labselect.batch.entity.BatchSession;
import com.goary.labselect.batch.entity.BatchTeacher;
import com.goary.labselect.batch.entity.CourseBatch;
import com.goary.labselect.batch.mapper.BatchSessionMapper;
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
import org.springframework.transaction.annotation.Transactional;
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

@Tag(name = "教师-批次管理", description = "仅 teacher")
@RestController
@RequestMapping("/api")
@RequireRole("teacher")
@RequiredArgsConstructor
public class BatchController {

    private final CourseMapper courseMapper;
    private final CourseBatchMapper batchMapper;
    private final BatchTeacherMapper batchTeacherMapper;
    private final BatchSessionMapper batchSessionMapper;
    private final ClassroomMapper classroomMapper;
    private final SysUserMapper sysUserMapper;
    private final CourseSelectionMapper selectionMapper;

    @Operation(summary = "课程下的批次列表")
    @GetMapping("/courses/{courseId}/batches")
    public Result<List<BatchVO>> listByCourse(@PathVariable Long courseId) {
        List<CourseBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<CourseBatch>()
                .eq(CourseBatch::getCourseId, courseId)
                .orderByAsc(CourseBatch::getId));
        return Result.ok(batches.stream().map(this::toVO).collect(Collectors.toList()));
    }

    @Operation(summary = "创建批次（含多个课次，容量取教室容量）")
    @PostMapping("/batches")
    @Transactional
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
        saveSessions(batch.getId(), form.getSessions());
        return Result.ok();
    }

    @Operation(summary = "编辑批次及课次（仅草稿）")
    @PutMapping("/batches/{id}")
    @Transactional
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody BatchForm form) {
        CourseBatch batch = mustOwnBatch(id);
        if (!"draft".equals(batch.getStatus())) {
            throw new BizException("仅草稿状态的批次可编辑");
        }
        checkBatchNoUnique(form.getCourseId(), form.getBatchNo(), id);
        applyAndCheck(batch, form);
        batchMapper.updateById(batch);
        saveTeachers(id, form.getTeacherIds());
        saveSessions(id, form.getSessions());
        return Result.ok();
    }

    @Operation(summary = "发布批次")
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

    @Operation(summary = "开放选课")
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

    @Operation(summary = "关闭选课")
    @PostMapping("/batches/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        CourseBatch batch = mustOwnBatch(id);
        batch.setSelectOpen(0);
        batch.setStatus("closed");
        batchMapper.updateById(batch);
        return Result.ok();
    }

    @Operation(summary = "删除批次（仅草稿）")
    @DeleteMapping("/batches/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id) {
        CourseBatch batch = mustOwnBatch(id);
        if (!"draft".equals(batch.getStatus())) {
            throw new BizException("仅草稿状态的批次可删除");
        }
        batchMapper.deleteById(id);
        batchTeacherMapper.physicalDeleteByBatchId(id);
        batchSessionMapper.physicalDeleteByBatchId(id);
        return Result.ok();
    }

    @Operation(summary = "批次选课名单")
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
                    "employeeNo", u == null ? "" : u.getEmployeeNo(),
                    "username", u == null ? "" : u.getUsername(),
                    "className", u == null || u.getClassName() == null ? "" : u.getClassName(),
                    "selectTime", s.getSelectTime().toString());
        }).collect(Collectors.toList());
        return Result.ok(rows);
    }

    private void applyAndCheck(CourseBatch batch, BatchForm form) {
        Classroom classroom = classroomMapper.selectById(form.getClassroomId());
        if (classroom == null || classroom.getStatus() != 1) {
            throw new BizException("教室不存在或已停用");
        }
        validateTeachers(form.getTeacherIds());
        batch.setCourseId(form.getCourseId());
        batch.setBatchNo(form.getBatchNo());
        batch.setClassroomId(form.getClassroomId());
        batch.setCapacity(classroom.getCapacity());

        List<SessionForm> sessions = sortedSessions(form.getSessions());
        for (int i = 0; i < sessions.size(); i++) {
            SessionForm session = sessions.get(i);
            if (!session.getEndTime().isAfter(session.getStartTime())) {
                throw new BizException("第" + (i + 1) + "个课次的结束时间必须晚于开始时间");
            }
            if (i > 0 && sessions.get(i - 1).getEndTime().isAfter(session.getStartTime())) {
                throw new BizException("同一批次内的课次时间不能重叠");
            }
            long classroomConflicts = batchSessionMapper.countClassroomConflict(
                    form.getClassroomId(), batch.getId(), session.getStartTime(), session.getEndTime());
            if (classroomConflicts > 0) {
                throw new BizException("第" + (i + 1) + "个课次的教室时间与其他批次冲突");
            }
            long teacherConflicts = batchSessionMapper.countTeacherConflict(
                    form.getTeacherIds(), batch.getId(), session.getStartTime(), session.getEndTime());
            if (teacherConflicts > 0) {
                throw new BizException("第" + (i + 1) + "个课次的授课教师时间与其他批次冲突");
            }
        }
    }

    private void validateTeachers(List<Long> teacherIds) {
        for (Long teacherId : teacherIds.stream().distinct().collect(Collectors.toList())) {
            SysUser teacher = sysUserMapper.selectById(teacherId);
            if (teacher == null || !"teacher".equals(teacher.getRole())) {
                throw new BizException("授课教师不存在：" + teacherId);
            }
        }
    }

    private void saveTeachers(Long batchId, List<Long> teacherIds) {
        batchTeacherMapper.physicalDeleteByBatchId(batchId);
        for (Long teacherId : teacherIds.stream().distinct().collect(Collectors.toList())) {
            BatchTeacher bt = new BatchTeacher();
            bt.setBatchId(batchId);
            bt.setTeacherId(teacherId);
            batchTeacherMapper.insert(bt);
        }
    }

    private void saveSessions(Long batchId, List<SessionForm> sessionForms) {
        batchSessionMapper.physicalDeleteByBatchId(batchId);
        List<SessionForm> sessionFormsSorted = sortedSessions(sessionForms);
        for (int i = 0; i < sessionFormsSorted.size(); i++) {
            SessionForm sessionForm = sessionFormsSorted.get(i);
            BatchSession session = new BatchSession();
            session.setBatchId(batchId);
            session.setSessionNo(i + 1);
            session.setStartTime(sessionForm.getStartTime());
            session.setEndTime(sessionForm.getEndTime());
            batchSessionMapper.insert(session);
        }
    }

    private List<SessionForm> sortedSessions(List<SessionForm> sessions) {
        return sessions.stream()
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());
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
        vo.setTeacherIds(teacherIds);
        vo.setTeacherNames(teacherIds.isEmpty() ? List.of()
                : sysUserMapper.selectBatchIds(teacherIds).stream().map(SysUser::getUsername).collect(Collectors.toList()));
        vo.setSessions(batchSessionMapper.selectList(new LambdaQueryWrapper<BatchSession>()
                .eq(BatchSession::getBatchId, batch.getId())
                .orderByAsc(BatchSession::getSessionNo)));
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
        @NotEmpty(message = "至少需要一个课次")
        private List<@Valid SessionForm> sessions;
    }

    @Data
    public static class SessionForm {
        @NotNull(message = "课次开始时间不能为空")
        private LocalDateTime startTime;
        @NotNull(message = "课次结束时间不能为空")
        private LocalDateTime endTime;
    }
}
