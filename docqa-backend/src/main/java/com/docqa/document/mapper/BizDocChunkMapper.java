package com.docqa.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docqa.document.entity.BizDocChunk;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 文档切片 Mapper：混合检索双路 SQL 在 XML 中定义
 */
public interface BizDocChunkMapper extends BaseMapper<BizDocChunk> {

    @Delete("DELETE FROM biz_doc_chunk WHERE version_id = #{versionId}")
    int deleteByVersionId(@Param("versionId") Long versionId);

    int batchInsertWithTsv(@Param("chunks") List<BizDocChunk> chunks,
                           @Param("tsvTexts") List<String> tsvTexts);

    List<Map<String, Object>> searchByKeyword(@Param("tsQuery") String tsQuery,
                                              @Param("categoryId") Long categoryId,
                                              @Param("roleIds") List<Long> roleIds,
                                              @Param("limit") int limit);

    /**
     * 向量路候选召回（兼容无 pgvector 环境）：
     * SQL 仅做权限/状态同构过滤拉候选（含 embedding 文本），余弦排序由应用层完成。
     */
    List<Map<String, Object>> searchByVectorCandidates(@Param("categoryId") Long categoryId,
                                                       @Param("roleIds") List<Long> roleIds,
                                                       @Param("limit") int limit);

    @Select("SELECT ts_headline('simple', #{content}, to_tsquery('simple', #{tsQuery}), 'StartSel=<em class=\"hl\">, StopSel=</em>')")
    String headline(@Param("content") String content, @Param("tsQuery") String tsQuery);

    @Update("UPDATE biz_doc_chunk SET is_active = false WHERE doc_id = #{docId} AND version_id <> #{versionId}")
    int deactivateOtherVersions(@Param("docId") Long docId, @Param("versionId") Long versionId);
}
