package com.docqa.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName(value = "sys_operation_log", autoResultMap = true)
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String username;
    private String module;
    private String operation;
    private String requestMethod;
    private String requestPath;
    private String params;
    private Integer resultCode;
    private String errorMsg;
    private String ip;
    private Integer costMs;
    private OffsetDateTime createdAt;
}
