package com.docqa.stats.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 统计分析 Mapper（实时聚合；数据量大时切换 biz_stats_daily 汇总表）
 */
public interface BizStatsMapper {

    @Select("SELECT count(*) AS doc_count, " +
            "count(*) FILTER (WHERE status = 'PUBLISHED') AS published_count, " +
            "count(*) FILTER (WHERE status = 'PENDING_AUDIT') AS pending_audit_count " +
            "FROM biz_document WHERE deleted_at IS NULL")
    Map<String, Object> docOverview();

    @Select("SELECT count(*) FROM biz_qa_log WHERE created_at >= current_date")
    long todayQaCount();

    @Select("SELECT count(*) FROM biz_doc_version WHERE index_status = 'FAILED'")
    long failedIndexCount();

    @Select("SELECT LEFT(question, 24) AS question, count(*) AS count FROM biz_qa_log " +
            "WHERE created_at >= now() - make_interval(days => #{days}) AND status = 'DONE' " +
            "GROUP BY LEFT(question, 24) ORDER BY count DESC LIMIT #{topN}")
    List<Map<String, Object>> hotQuestions(@Param("days") int days, @Param("topN") int topN);

    @Select("SELECT d.id AS doc_id, d.title AS doc_title, count(a.id) AS quote_count FROM biz_document d " +
            "JOIN biz_access_log a ON a.doc_id = d.id AND a.action = 'QUOTE' " +
            "WHERE d.deleted_at IS NULL AND a.created_at >= now() - make_interval(days => #{days}) " +
            "GROUP BY d.id, d.title ORDER BY quote_count DESC LIMIT #{topN}")
    List<Map<String, Object>> docQuotes(@Param("days") int days, @Param("topN") int topN);

    @Select("SELECT id AS doc_id, title AS doc_title, quote_count FROM biz_document " +
            "WHERE deleted_at IS NULL AND status = 'PUBLISHED' ORDER BY quote_count DESC LIMIT #{topN}")
    List<Map<String, Object>> docQuotesFallback(@Param("topN") int topN);

    @Select("SELECT to_char(date_trunc('month', created_at), 'YYYY-MM') AS month, count(*) AS count " +
            "FROM biz_document WHERE deleted_at IS NULL " +
            "AND created_at >= date_trunc('month', now()) - (#{months} || ' months')::interval " +
            "GROUP BY 1 ORDER BY 1")
    List<Map<String, Object>> uploadTrend(@Param("months") int months);

    @Select("SELECT DISTINCT query_text FROM biz_access_log " +
            "WHERE action = 'SEARCH' AND query_text ILIKE '%' || #{q} || '%' LIMIT 8")
    List<String> searchSuggest(@Param("q") String q);
}
