package com.docqa.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docqa.qa.entity.BizQaLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface BizQaMapper extends BaseMapper<BizQaLog> {

    @Insert("INSERT INTO biz_qa_session(session_id, user_id, title) VALUES(#{sessionId}, #{userId}, #{title})")
    void insertSession(@Param("sessionId") String sessionId, @Param("userId") Long userId, @Param("title") String title);

    @Update("UPDATE biz_qa_session SET title = #{title}, updated_at = now() WHERE session_id = #{sessionId}")
    void updateSessionTitle(@Param("sessionId") String sessionId, @Param("title") String title);

    @Select("SELECT s.session_id, s.title, s.created_at, s.updated_at FROM biz_qa_session s " +
            "WHERE s.user_id = #{userId} ORDER BY s.updated_at DESC")
    List<Map<String, Object>> selectSessions(@Param("userId") Long userId);

    @Select("SELECT EXISTS(SELECT 1 FROM biz_qa_session WHERE session_id = #{sessionId} AND user_id = #{userId})")
    boolean ownsSession(@Param("sessionId") String sessionId, @Param("userId") Long userId);

    @Update("DELETE FROM biz_qa_session WHERE session_id = #{sessionId} AND user_id = #{userId}")
    int deleteSession(@Param("sessionId") String sessionId, @Param("userId") Long userId);

    @Insert("INSERT INTO biz_qa_log(session_id, user_id, question, model, status, sources) " +
            "VALUES(#{sessionId}, #{userId}, #{question}, #{model}, 'ANSWERING', #{sources})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertQaLog(BizQaLog log);

    @Update("UPDATE biz_qa_log SET answer = #{answer}, status = #{status}, latency_ms = #{latencyMs}, " +
            "first_token_ms = #{firstTokenMs}, prompt_tokens = #{promptTokens}, " +
            "completion_tokens = #{completionTokens}, sources = #{sources}, updated_at = NOW() " +
            "WHERE id = #{id}")
    void finishQaLog(BizQaLog log);

    @Update("UPDATE biz_qa_log SET status = 'FAILED', updated_at = now() WHERE id = #{id}")
    void failQaLog(@Param("id") Long id);

    @Update("UPDATE biz_qa_log SET feedback = #{feedback} WHERE id = #{id}")
    void updateFeedback(@Param("id") Long id, @Param("feedback") int feedback);

    @Select("SELECT role, content, sources, qa_id, feedback FROM (" +
            "SELECT 'user' AS role, question AS content, NULL AS sources, NULL AS qa_id, 0 AS feedback, created_at, id " +
            "FROM biz_qa_log WHERE session_id = #{sessionId} " +
            "UNION ALL " +
            "SELECT 'assistant', answer, sources, id, feedback, created_at, id " +
            "FROM biz_qa_log WHERE session_id = #{sessionId}) t ORDER BY created_at ASC, id ASC")
    List<Map<String, Object>> selectMessages(@Param("sessionId") String sessionId);

    @Select("SELECT q.created_at, u.nickname AS username, q.question, LEFT(q.answer, 120) AS answer, " +
            "q.status, q.latency_ms, q.first_token_ms FROM biz_qa_log q " +
            "LEFT JOIN sys_user u ON u.id = q.user_id ORDER BY q.created_at DESC LIMIT #{size} OFFSET #{offset}")
    List<Map<String, Object>> selectQaLogs(@Param("offset") long offset, @Param("size") long size);

    @Select("SELECT count(*) FROM biz_qa_log")
    long countQaLogs();
}
