package com.docqa.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docqa.document.entity.BizAccessLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 收藏/订阅/通知/访问日志 聚合 Mapper
 */
public interface BizDocSocialMapper extends BaseMapper<BizAccessLog> {

    @Insert("INSERT IGNORE INTO biz_favorite(user_id, doc_id) VALUES(#{userId}, #{docId})")
    int favorite(@Param("userId") Long userId, @Param("docId") Long docId);

    @Delete("DELETE FROM biz_favorite WHERE user_id = #{userId} AND doc_id = #{docId}")
    int unfavorite(@Param("userId") Long userId, @Param("docId") Long docId);

    @Select("SELECT EXISTS(SELECT 1 FROM biz_favorite WHERE user_id = #{userId} AND doc_id = #{docId})")
    boolean isFavorited(@Param("userId") Long userId, @Param("docId") Long docId);

    @Select("SELECT f.id, f.doc_id, f.created_at, d.title AS doc_title FROM biz_favorite f " +
            "JOIN biz_document d ON d.id = f.doc_id AND d.deleted_at IS NULL " +
            "WHERE f.user_id = #{userId} ORDER BY f.created_at DESC LIMIT #{size} OFFSET #{offset}")
    List<Map<String, Object>> selectFavorites(@Param("userId") Long userId,
                                              @Param("offset") long offset, @Param("size") long size);

    @Select("SELECT count(*) FROM biz_favorite WHERE user_id = #{userId}")
    long countFavorites(@Param("userId") Long userId);

    @Insert("INSERT IGNORE INTO biz_subscription(user_id, sub_type, target_id) VALUES(#{userId}, #{subType}, #{targetId})")
    void subscribe(@Param("userId") Long userId, @Param("subType") String subType, @Param("targetId") Long targetId);

    @Delete("DELETE FROM biz_subscription WHERE id = #{id} AND user_id = #{userId}")
    void unsubscribe(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT s.id, s.sub_type, s.target_id, s.created_at, " +
            "CASE WHEN s.sub_type = 'DOC' THEN d.title ELSE c.category_name END AS target_name " +
            "FROM biz_subscription s " +
            "LEFT JOIN biz_document d ON s.sub_type = 'DOC' AND d.id = s.target_id " +
            "LEFT JOIN biz_category c ON s.sub_type = 'CATEGORY' AND c.id = s.target_id " +
            "WHERE s.user_id = #{userId} ORDER BY s.created_at DESC")
    List<Map<String, Object>> selectSubscriptions(@Param("userId") Long userId);

    @Select("SELECT user_id FROM biz_subscription WHERE sub_type = #{subType} AND target_id = #{targetId}")
    List<Long> selectSubscriberIds(@Param("subType") String subType, @Param("targetId") Long targetId);

    @Insert("INSERT INTO biz_notify(user_id, title, content, doc_id) VALUES(#{userId}, #{title}, #{content}, #{docId})")
    void insertNotify(@Param("userId") Long userId, @Param("title") String title,
                      @Param("content") String content, @Param("docId") Long docId);

    @Select("SELECT n.*, d.title AS doc_title FROM biz_notify n LEFT JOIN biz_document d ON d.id = n.doc_id " +
            "WHERE n.user_id = #{userId} ORDER BY n.created_at DESC LIMIT #{size} OFFSET #{offset}")
    List<Map<String, Object>> selectNotifies(@Param("userId") Long userId,
                                             @Param("offset") long offset, @Param("size") long size);

    @Select("SELECT count(*) FROM biz_notify WHERE user_id = #{userId}")
    long countNotifies(@Param("userId") Long userId);

    @Update("UPDATE biz_notify SET is_read = true WHERE id = #{id} AND user_id = #{userId}")
    void markNotifyRead(@Param("id") Long id, @Param("userId") Long userId);

    @Insert("INSERT INTO biz_access_log(user_id, doc_id, action, query_text, ip) " +
            "VALUES(#{userId}, #{docId}, #{action}, #{queryText}, #{ip})")
    void insertAccessLog(@Param("userId") Long userId, @Param("docId") Long docId,
                         @Param("action") String action, @Param("queryText") String queryText,
                         @Param("ip") String ip);

    @Select("SELECT a.created_at, u.nickname AS username, a.action, d.title AS doc_title, a.query_text, a.ip " +
            "FROM biz_access_log a LEFT JOIN sys_user u ON u.id = a.user_id " +
            "LEFT JOIN biz_document d ON d.id = a.doc_id ORDER BY a.created_at DESC LIMIT #{size} OFFSET #{offset}")
    List<Map<String, Object>> selectAccessLogs(@Param("offset") long offset, @Param("size") long size);

    @Select("SELECT count(*) FROM biz_access_log")
    long countAccessLogs();
}
