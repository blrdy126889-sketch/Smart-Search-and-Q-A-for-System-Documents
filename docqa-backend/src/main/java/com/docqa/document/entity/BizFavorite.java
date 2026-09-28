package com.docqa.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("biz_favorite")
public class BizFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long docId;
    private OffsetDateTime createdAt;
}
