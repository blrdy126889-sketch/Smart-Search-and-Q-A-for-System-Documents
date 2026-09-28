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

    @Insert("INSERT INTO biz_category_perm(category_id, role_id) VALUES(#{categoryId}, #{roleId}) ON CONFLICT DO NOTHING")
    void insertCategoryPerm(@Param("categoryId") Long categoryId, @Param("roleId") Long roleId);

    @Delete("DELETE FROM biz_category_perm WHERE category_id = #{categoryId}")
    void deleteCategoryPerms(@Param("categoryId") Long categoryId);

    @Select("SELECT role_id FROM biz_category_perm WHERE category_id = #{categoryId}")
    List<Long> selectPermRoleIds(@Param("categoryId") Long categoryId);

    @Update("UPDATE biz_category c SET doc_count = (" +
            "SELECT count(*) FROM biz_document d WHERE d.category_id = c.id AND d.deleted_at IS NULL) " +
            "WHERE c.id = #{categoryId}")
    void refreshDocCount(@Param("categoryId") Long categoryId);
}
