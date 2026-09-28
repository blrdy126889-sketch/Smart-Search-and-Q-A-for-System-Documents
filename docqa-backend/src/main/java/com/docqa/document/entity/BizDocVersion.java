package com.docqa.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("biz_doc_version")
public class BizDocVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long docId;
    private String versionNo;
    private String titleSnapshot;
    private String filePath;
    private String fileHash;
    private String plainTextPath;
    private Integer charCount;
    private Integer chunkCount;
    private String indexStatus;
    private String failReason;
    private String summary;
    private String changeLog;
    private Long createdBy;
    private OffsetDateTime publishedAt;
    private Integer retryCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
