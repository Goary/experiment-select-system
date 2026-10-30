package com.goary.labselect.batch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.goary.labselect.batch.entity.BatchSession;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface BatchSessionMapper extends BaseMapper<BatchSession> {

    @Select("SELECT COUNT(*) FROM batch_session s " +
            "JOIN course_batch b ON b.id = s.batch_id AND b.deleted = 0 " +
            "WHERE s.deleted = 0 AND b.classroom_id = #{classroomId} AND b.status <> 'closed' " +
            "AND (#{excludeBatchId} IS NULL OR b.id <> #{excludeBatchId}) " +
            "AND s.start_time < #{endTime} AND s.end_time > #{startTime}")
    long countClassroomConflict(@Param("classroomId") Long classroomId,
                                @Param("excludeBatchId") Long excludeBatchId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    @Select("<script>" +
            "SELECT COUNT(*) FROM batch_session s " +
            "JOIN course_batch b ON b.id = s.batch_id AND b.deleted = 0 " +
            "JOIN batch_teacher bt ON bt.batch_id = b.id AND bt.deleted = 0 " +
            "WHERE s.deleted = 0 AND b.status &lt;&gt; 'closed' " +
            "AND (#{excludeBatchId} IS NULL OR b.id &lt;&gt; #{excludeBatchId}) " +
            "AND bt.teacher_id IN " +
            "<foreach collection='teacherIds' item='teacherId' open='(' separator=',' close=')'>#{teacherId}</foreach> " +
            "AND s.start_time &lt; #{endTime} AND s.end_time &gt; #{startTime}" +
            "</script>")
    long countTeacherConflict(@Param("teacherIds") List<Long> teacherIds,
                              @Param("excludeBatchId") Long excludeBatchId,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COUNT(*) FROM batch_session target " +
            "JOIN batch_session mine ON target.start_time < mine.end_time AND target.end_time > mine.start_time " +
            "JOIN course_selection cs ON cs.batch_id = mine.batch_id AND cs.deleted = 0 " +
            "WHERE target.deleted = 0 AND mine.deleted = 0 AND target.batch_id = #{targetBatchId} " +
            "AND cs.student_id = #{studentId} AND cs.status = 'selected'")
    long countStudentConflict(@Param("studentId") Long studentId, @Param("targetBatchId") Long targetBatchId);

    @Delete("DELETE FROM batch_session WHERE batch_id = #{batchId}")
    int physicalDeleteByBatchId(@Param("batchId") Long batchId);
}
