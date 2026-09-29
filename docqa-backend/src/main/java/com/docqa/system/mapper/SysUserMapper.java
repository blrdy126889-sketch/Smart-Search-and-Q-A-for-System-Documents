package com.docqa.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docqa.system.entity.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface SysUserMapper extends BaseMapper<SysUser> {

    @Insert("INSERT IGNORE INTO sys_user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    void deleteUserRoles(@Param("userId") Long userId);

    @Select("SELECT r.id, r.role_name, r.role_code, r.role_desc FROM sys_role r " +
            "JOIN sys_user_role ur ON ur.role_id = r.id WHERE ur.user_id = #{userId}")
    List<Map<String, Object>> selectRolesByUserId(@Param("userId") Long userId);

    @Select("SELECT role_code FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id WHERE ur.user_id = #{userId}")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
