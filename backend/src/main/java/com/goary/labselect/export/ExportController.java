package com.goary.labselect.export;

import com.alibaba.excel.EasyExcel;
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
import com.goary.labselect.course.entity.Course;
import com.goary.labselect.course.mapper.CourseMapper;
import com.goary.labselect.selection.entity.CourseSelection;
import com.goary.labselect.selection.mapper.CourseSelectionMapper;
import com.goary.labselect.user.entity.SysUser;
import com.goary.labselect.user.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/export")
@RequireRole("teacher")
@RequiredArgsConstructor
public class ExportController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CourseSelectionMapper selectionMapper;
    private final CourseBatchMapper batchMapper;
    private final BatchTeacherMapper batchTeacherMapper;
    private final CourseMapper courseMapper;
    private final ClassroomMapper classroomMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * by=teacher：导出当前教师授课的所有批次学生名单
     * by=batch&batchId=N：导出指定批次（实验+批次+教室维度）学生名单
     */
    @GetMapping("/selections")
    public void export(@RequestParam String by,
                       @RequestParam(required = false) Long batchId,
                       HttpServletResponse response) throws IOException {
        Long teacherId = UserContext.getUserId();
        List<CourseBatch> batches;
        String filename;
        if ("teacher".equals(by)) {
            List<Long> batchIds = batchTeacherMapper.selectList(new LambdaQueryWrapper<BatchTeacher>()
                            .eq(BatchTeacher::getTeacherId, teacherId))
                    .stream().map(BatchTeacher::getBatchId).collect(Collectors.toList());
            batches = batchIds.isEmpty() ? List.of() : batchMapper.selectBatchIds(batchIds);
            filename = "选课名单-按教师.xlsx";
        } else if ("batch".equals(by)) {
            if (batchId == null) {
                throw new BizException("缺少参数 batchId");
            }
            CourseBatch batch = batchMapper.selectById(batchId);
            if (batch == null) {
                throw new BizException("批次不存在");
            }
            Long count = batchTeacherMapper.selectCount(new LambdaQueryWrapper<BatchTeacher>()
                    .eq(BatchTeacher::getBatchId, batchId)
                    .eq(BatchTeacher::getTeacherId, teacherId));
            Course course = courseMapper.selectById(batch.getCourseId());
            if (count == 0 && (course == null || !course.getCreatorId().equals(teacherId))) {
                throw new BizException(403, "只能导出自己授课的批次");
            }
            batches = List.of(batch);
            filename = "选课名单-" + (course == null ? "" : course.getName()) + "-" + batch.getBatchNo() + ".xlsx";
        } else {
            throw new BizException("参数 by 仅支持 teacher / batch");
        }

        writeExcel(response, buildRows(batches), filename);
    }

    private List<SelectionExportRow> buildRows(List<CourseBatch> batches) {
        if (batches.isEmpty()) {
            return List.of();
        }
        List<Long> batchIds = batches.stream().map(CourseBatch::getId).collect(Collectors.toList());
        Map<Long, CourseBatch> batchMap = batches.stream().collect(Collectors.toMap(CourseBatch::getId, b -> b));
        Map<Long, Course> courseMap = courseMapper.selectBatchIds(
                        batches.stream().map(CourseBatch::getCourseId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Course::getId, c -> c));
        Map<Long, Classroom> classroomMap = classroomMapper.selectBatchIds(
                        batches.stream().map(CourseBatch::getClassroomId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Classroom::getId, c -> c));

        List<CourseSelection> selections = selectionMapper.selectList(new LambdaQueryWrapper<CourseSelection>()
                .in(CourseSelection::getBatchId, batchIds)
                .eq(CourseSelection::getStatus, "selected")
                .orderByAsc(CourseSelection::getBatchId)
                .orderByAsc(CourseSelection::getSelectTime));
        if (selections.isEmpty()) {
            return List.of();
        }
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(
                        selections.stream().map(CourseSelection::getStudentId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        return selections.stream().map(s -> {
            CourseBatch b = batchMap.get(s.getBatchId());
            Course c = courseMap.get(b.getCourseId());
            Classroom room = classroomMap.get(b.getClassroomId());
            SysUser u = userMap.get(s.getStudentId());
            SelectionExportRow row = new SelectionExportRow();
            row.setUsername(u == null ? "" : u.getUsername());
            row.setRealName(u == null ? "" : u.getRealName());
            row.setClassName(u == null || u.getClassName() == null ? "" : u.getClassName());
            row.setCourseCode(c == null ? "" : c.getCourseCode());
            row.setCourseName(c == null ? "" : c.getName());
            row.setBatchNo(b.getBatchNo());
            row.setClassroomName(room == null ? "" : room.getName());
            row.setTimeRange(b.getStartTime().format(FMT) + " ~ " + b.getEndTime().format(FMT));
            row.setSelectTime(s.getSelectTime().format(FMT));
            return row;
        }).collect(Collectors.toList());
    }

    private void writeExcel(HttpServletResponse response, List<SelectionExportRow> rows, String filename) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                + URLEncoder.encode(filename, StandardCharsets.UTF_8));
        EasyExcel.write(response.getOutputStream(), SelectionExportRow.class).sheet("选课名单").doWrite(rows);
    }
}
