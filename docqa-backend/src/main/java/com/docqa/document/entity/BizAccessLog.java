package com.docqa.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 访问日志（按月分区表）
 */
@Data
@TableName("biz_access_log")
public class BizAccessLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long docId;
    private String action;
    private String queryText;
    private String ip;
    private OffsetDateTime createdAt;
}
