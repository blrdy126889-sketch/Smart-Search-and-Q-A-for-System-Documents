package com.docqa.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docqa.document.entity.BizDocument;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface BizDocumentMapper extends BaseMapper<BizDocument> {

    @Update("UPDATE biz_document SET view_count = view_count + 1 WHERE id = #{docId}")
    void increaseViewCount(@Param("docId") Long docId);

    @Update("UPDATE biz_document SET quote_count = quote_count + 1 WHERE id = #{docId}")
    void increaseQuoteCount(@Param("docId") Long docId);

    @Update("UPDATE biz_document SET favorite_count = favorite_count + #{delta} WHERE id = #{docId}")
    void adjustFavoriteCount(@Param("docId") Long docId, @Param("delta") int delta);

    List<Map<String, Object>> selectDocPage(@Param("keyword") String keyword, @Param("categoryId") Long categoryId,
                                            @Param("status") String status,
                                            @Param("page") long page, @Param("size") long size);

    long countDocPage(@Param("keyword") String keyword, @Param("categoryId") Long categoryId,
                      @Param("status") String status);
}
