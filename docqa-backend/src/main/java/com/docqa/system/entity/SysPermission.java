package com.docqa.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sys_permission")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;
    private String permName;
    private String permCode;
    private Integer permType;
    private String routePath;
    private Integer sort;
    private Integer status;
    private OffsetDateTime createdAt;
}
