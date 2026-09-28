package com.docqa.document.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.docqa.framework.vector.PgVectorTypeHandler;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@TableName(value = "biz_doc_chunk", autoResultMap = true)
public class BizDocChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long docId;
    private Long versionId;
    private Integer chunkIndex;
    private String content;
    private String headingPath;
    private Integer pageNo;
    private Integer charCount;

    @TableField(typeHandler = PgVectorTypeHandler.class)
    private List<Double> embedding;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private String tsv;

    private Boolean isActive;
    private OffsetDateTime createdAt;
}
