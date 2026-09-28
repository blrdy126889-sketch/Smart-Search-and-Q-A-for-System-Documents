package com.docqa.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docqa.document.entity.BizDocVersion;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface BizDocVersionMapper extends BaseMapper<BizDocVersion> {

    @Select("SELECT v.*, u.nickname AS creator_name FROM biz_doc_version v " +
            "LEFT JOIN sys_user u ON u.id = v.created_by WHERE v.doc_id = #{docId} ORDER BY v.id DESC")
    List<Map<String, Object>> selectVersionsByDocId(@Param("docId") Long docId);

    @Update("UPDATE biz_doc_version SET index_status = #{to}, fail_reason = #{failReason}, " +
            "retry_count = retry_count + #{retryInc}, updated_at = now() " +
            "WHERE id = #{versionId} AND index_status = #{from}")
    int casIndexStatus(@Param("versionId") Long versionId, @Param("from") String from,
                       @Param("to") String to, @Param("failReason") String failReason,
                       @Param("retryInc") int retryInc);

    @Select("SELECT * FROM biz_doc_version WHERE index_status IN ('PENDING','FAILED') " +
            "AND updated_at < now() - interval '10 minutes' AND retry_count < 5")
    List<BizDocVersion> selectRetryCandidates();

    @Select("SELECT 'v' || (COALESCE(MAX(SUBSTRING(version_no FROM '[0-9]+')::int), 0) + 1) || '.0' " +
            "FROM biz_doc_version WHERE doc_id = #{docId}")
    String nextVersionNo(@Param("docId") Long docId);
}
