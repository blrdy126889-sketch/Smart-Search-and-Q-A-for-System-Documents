package com.docqa.category.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("biz_category")
public class BizCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;
    private String ancestors;
    private String categoryName;
    private String categoryCode;
    private Integer sortOrder;
    private Integer docCount;
    private Integer status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
