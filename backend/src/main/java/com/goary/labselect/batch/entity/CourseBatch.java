package com.goary.labselect.batch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_batch")
public class CourseBatch {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private String batchNo;
    private Long classroomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer capacity;
    private Integer selectedCount;
    private Integer selectOpen;
    private String status;
    @TableLogic
    @JsonIgnore
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
