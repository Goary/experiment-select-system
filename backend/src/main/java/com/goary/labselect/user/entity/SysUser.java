package com.goary.labselect.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String employeeNo;
    @JsonIgnore
    private String password;
    private String username;
    private String role;
    private String className;
    private Integer status;
    @TableLogic
    @JsonIgnore
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
