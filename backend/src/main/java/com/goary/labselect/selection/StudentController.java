package com.goary.labselect.selection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.goary.labselect.auth.UserContext;
import com.goary.labselect.batch.BatchVO;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
@RequireRole("student")
@RequiredArgsConstructor
public class StudentController {

    private final CourseMapper courseMapper;
    private final CourseBatchMapper batchMapper;
    private final BatchTeacherMapper batchTeacherMapper;
    private final ClassroomMapper classroomMapper;
    private final SysUserMapper sysUserMapper;
    private final CourseSelectionMapper selectionMapper;

    @GetMapping("/courses/open")
    public Result<List<Map<String, Object>>> openCourses() {
        List<CourseBatch> openBatches = batchMapper.selectList(new LambdaQueryWrapper<CourseBatch>()
                .eq(CourseBatch::getSelectOpen, 1)
                .eq(CourseBatch::getStatus, "published")
                .orderByAsc(CourseBatch::getStartTime));
        List<Long> courseIds = openBatches.stream().map(CourseBatch::getCourseId).distinct().collect(Collectors.toList());
        if (courseIds.isEmpty()) {
            return Result.ok(List.of());
        }
        Map<Long, Course> courseMap = courseMapper.selectBatchIds(courseIds).stream()
                .filter(c -> "published".equals(c.getStatus()))
                .collect(Collectors.toMap(Course::getId, c -> c));
        // 该生已选中的批次（用于前端标记）
        Map<Long, Long> mySelected = mySelectedBatchByCourse();

        List<Map<String, Object>> result = openBatches.stream()
                .filter(b -> courseMap.containsKey(b.getCourseId()))
                .collect(Collectors.groupingBy(CourseBatch::getCourseId))
                .entrySet().stream().map(e -> {
                    Course c = courseMap.get(e.getKey());
                    return Map.<String, Object>of(
                            "courseId", c.getId(),
                            "courseCode", c.getCourseCode(),
                            "courseName", c.getName(),
                            "description", c.getDescription() == null ? "" : c.getDescription(),
                            "batches", e.getValue().stream().map(this::toVO).collect(Collectors.toList()),
                            "mySelectedBatchId", mySelected.getOrDefault(c.getId(), 0L));
                }).collect(Collectors.toList());
        return Result.ok(result);
    }

    @PostMapping("/select/{batchId}")
    @Transactional
    public Result<Void> select(@PathVariable Long batchId) {
        Long studentId = UserContext.getUserId();
        CourseBatch batch = batchMapper.selectById(batchId);
        if (batch == null || !"published".equals(batch.getStatus()) || batch.getSelectOpen() != 1) {
            throw new BizException("该批次未开放选课");
        }
        // 同一课程只能选中一个批次
        Long exists = selectionMapper.selectCount(new LambdaQueryWrapper<CourseSelection>()
                .eq(CourseSelection::getStudentId, studentId)
                .eq(CourseSelection::getCourseId, batch.getCourseId())
                .eq(CourseSelection::getStatus, "selected"));
        if (exists > 0) {
            throw new BizException("你已选过本课程的其他批次，如需更换请先退选");
        }
        // 时间段冲突校验
        List<CourseSelection> mine = selectionMapper.selectList(new LambdaQueryWrapper<CourseSelection>()
                .eq(CourseSelection::getStudentId, studentId)
                .eq(CourseSelection::getStatus, "selected"));
        if (!mine.isEmpty()) {
            List<Long> batchIds = mine.stream().map(CourseSelection::getBatchId).collect(Collectors.toList());
            Long conflicts = batchMapper.selectCount(new LambdaQueryWrapper<CourseBatch>()
                    .in(CourseBatch::getId, batchIds)
                    .lt(CourseBatch::getStartTime, batch.getEndTime())
                    .gt(CourseBatch::getEndTime, batch.getStartTime()));
            if (conflicts > 0) {
                throw new BizException("该批次时间与你已选课程冲突");
            }
        }
        // 原子占位，防并发超选
        if (batchMapper.incrSelectedCount(batchId) == 0) {
            throw new BizException("该批次名额已满或未开放选课");
        }
        CourseSelection selection = new CourseSelection();
        selection.setStudentId(studentId);
        selection.setCourseId(batch.getCourseId());
        selection.setBatchId(batchId);
        selection.setStatus("selected");
        selectionMapper.insert(selection);
        return Result.ok();
    }

    @PostMapping("/drop/{selectionId}")
    @Transactional
    public Result<Void> drop(@PathVariable Long selectionId) {
        CourseSelection selection = selectionMapper.selectById(selectionId);
        if (selection == null || !selection.getStudentId().equals(UserContext.getUserId())
                || !"selected".equals(selection.getStatus())) {
            throw new BizException("选课记录不存在");
        }
        CourseBatch batch = batchMapper.selectById(selection.getBatchId());
        if (batch != null && "closed".equals(batch.getStatus())) {
            throw new BizException("该批次已关闭，不能退选");
        }
        selection.setStatus("dropped");
        selectionMapper.updateById(selection);
        batchMapper.decrSelectedCount(selection.getBatchId());
        return Result.ok();
    }

    @GetMapping("/schedule")
    public Result<List<Map<String, Object>>> schedule() {
        List<CourseSelection> mine = selectionMapper.selectList(new LambdaQueryWrapper<CourseSelection>()
                .eq(CourseSelection::getStudentId, UserContext.getUserId())
                .eq(CourseSelection::getStatus, "selected"));
        List<Map<String, Object>> rows = mine.stream().map(s -> {
            CourseBatch batch = batchMapper.selectById(s.getBatchId());
            Course course = courseMapper.selectById(s.getCourseId());
            Classroom classroom = batch == null ? null : classroomMapper.selectById(batch.getClassroomId());
            List<String> teachers = batch == null ? List.of() : teacherNames(batch.getId());
            return Map.<String, Object>of(
                    "selectionId", s.getId(),
                    "courseName", course == null ? "" : course.getName(),
                    "courseCode", course == null ? "" : course.getCourseCode(),
                    "batchNo", batch == null ? "" : batch.getBatchNo(),
                    "batchStatus", batch == null ? "" : batch.getStatus(),
                    "classroomName", classroom == null ? "" : classroom.getName(),
                    "teacherNames", teachers,
                    "startTime", batch == null ? "" : batch.getStartTime().toString(),
                    "endTime", batch == null ? "" : batch.getEndTime().toString(),
                    "selectTime", s.getSelectTime().toString());
        }).sorted((a, b) -> ((String) a.get("startTime")).compareTo((String) b.get("startTime")))
                .collect(Collectors.toList());
        return Result.ok(rows);
    }

    private Map<Long, Long> mySelectedBatchByCourse() {
        return selectionMapper.selectList(new LambdaQueryWrapper<CourseSelection>()
                        .eq(CourseSelection::getStudentId, UserContext.getUserId())
                        .eq(CourseSelection::getStatus, "selected"))
                .stream().collect(Collectors.toMap(CourseSelection::getCourseId, CourseSelection::getBatchId));
    }

    private List<String> teacherNames(Long batchId) {
        List<Long> teacherIds = batchTeacherMapper.selectList(
                        new LambdaQueryWrapper<BatchTeacher>().eq(BatchTeacher::getBatchId, batchId))
                .stream().map(BatchTeacher::getTeacherId).collect(Collectors.toList());
        return teacherIds.isEmpty() ? List.of()
                : sysUserMapper.selectBatchIds(teacherIds).stream().map(SysUser::getRealName).collect(Collectors.toList());
    }

    private BatchVO toVO(CourseBatch batch) {
        BatchVO vo = new BatchVO();
        BeanUtils.copyProperties(batch, vo);
        Classroom classroom = classroomMapper.selectById(batch.getClassroomId());
        if (classroom != null) {
            vo.setClassroomName(classroom.getName());
            vo.setClassroomLocation(classroom.getLocation());
        }
        vo.setTeacherNames(teacherNames(batch.getId()));
        vo.setRemaining(batch.getCapacity() - batch.getSelectedCount());
        return vo;
    }
}
