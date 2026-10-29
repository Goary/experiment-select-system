package com.goary.labselect.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String courseCode;
    private String name;
    private String description;
    private BigDecimal credit;
    private Long creatorId;
    private String status;
    @TableLogic
    @JsonIgnore
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
