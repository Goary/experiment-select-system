package com.goary.labselect.batch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.goary.labselect.batch.entity.BatchTeacher;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface BatchTeacherMapper extends BaseMapper<BatchTeacher> {

    @Delete("DELETE FROM batch_teacher WHERE batch_id = #{batchId}")
    int physicalDeleteByBatchId(@Param("batchId") Long batchId);
}
