package com.goary.labselect.batch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("batch_session")
public class BatchSession {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Integer sessionNo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @TableLogic
    @JsonIgnore
    private Integer deleted;
    @JsonIgnore
    private LocalDateTime createTime;
    @JsonIgnore
    private LocalDateTime updateTime;
}
