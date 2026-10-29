package com.goary.labselect.batch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.goary.labselect.batch.entity.CourseBatch;
import org.apache.ibatis.annotations.Update;

public interface CourseBatchMapper extends BaseMapper<CourseBatch> {

    @Update("UPDATE course_batch SET selected_count = selected_count + 1 " +
            "WHERE id = #{id} AND deleted = 0 AND select_open = 1 AND selected_count < capacity")
    int incrSelectedCount(Long id);

    @Update("UPDATE course_batch SET selected_count = selected_count - 1 " +
            "WHERE id = #{id} AND selected_count > 0")
    int decrSelectedCount(Long id);
}
