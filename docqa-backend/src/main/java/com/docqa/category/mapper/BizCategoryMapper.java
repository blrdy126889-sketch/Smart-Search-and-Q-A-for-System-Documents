package com.docqa.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docqa.category.entity.BizCategory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface BizCategoryMapper extends BaseMapper<BizCategory> {

    @Insert("INSERT IGNORE INTO biz_category_perm(category_id, role_id) VALUES(#{categoryId}, #{roleId})")
    void insertCategoryPerm(@Param("categoryId") Long categoryId, @Param("roleId") Long roleId);

    @Delete("DELETE FROM biz_category_perm WHERE category_id = #{categoryId}")
    void deleteCategoryPerms(@Param("categoryId") Long categoryId);

    @Select("SELECT role_id FROM biz_category_perm WHERE category_id = #{categoryId}")
    List<Long> selectPermRoleIds(@Param("categoryId") Long categoryId);

    /** 一次取全部分类授权映射（树构建防 N+1） */
    @Select("SELECT category_id, role_id FROM biz_category_perm")
    List<java.util.Map<String, Object>> selectAllPerms();

    @Update("UPDATE biz_category c SET doc_count = (" +
            "SELECT count(*) FROM biz_document d WHERE d.category_id = c.id AND d.deleted_at IS NULL) " +
            "WHERE c.id = #{categoryId}")
    void refreshDocCount(@Param("categoryId") Long categoryId);
}
