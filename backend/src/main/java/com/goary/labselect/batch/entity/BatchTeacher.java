package com.goary.labselect.batch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
@TableName("batch_teacher")
public class BatchTeacher {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Long teacherId;
    @TableLogic
    @JsonIgnore
    private Integer deleted;
}
