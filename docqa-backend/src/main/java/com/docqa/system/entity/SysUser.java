package com.docqa.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;
    private String email;
    private String phone;
    private Integer status;
    private OffsetDateTime lastLoginAt;

    /** 逻辑删：null=未删（IS NULL 判断），now()=删除时间戳 */
    @TableLogic(value = "null", delval = "now()")
    private OffsetDateTime deletedAt;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
