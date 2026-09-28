package com.docqa.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@TableName("biz_document")
public class BizDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String docCode;
    private String title;
    private Long categoryId;
    private Long currentVersionId;
    private String status;
    private String summary;
    private String sourceType;
    private Long fileSize;
    private Integer secretLevel;
    private LocalDate effectiveDate;
    private Integer viewCount;
    private Integer quoteCount;
    private Integer favoriteCount;
    private Long ownerId;
    private Long auditedBy;
    private OffsetDateTime auditedAt;
    private String auditRemark;

    /** 逻辑删：null=未删（IS NULL 判断），now()=删除时间戳 */
    @TableLogic(value = "null", delval = "now()")
    private OffsetDateTime deletedAt;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
