package com.docqa.qa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName(value = "biz_qa_log", autoResultMap = true)
public class BizQaLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;
    private Long userId;
    private String question;
    private String answer;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer latencyMs;
    private Integer firstTokenMs;
    private String status;
    private String sources;
    private Integer feedback;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
