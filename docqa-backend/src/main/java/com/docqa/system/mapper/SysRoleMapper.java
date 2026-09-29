package com.docqa.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docqa.system.entity.SysRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Insert("INSERT IGNORE INTO sys_role_permission(role_id, permission_id) VALUES(#{roleId}, #{permId})")
    void insertRolePerm(@Param("roleId") Long roleId, @Param("permId") Long permId);

    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    void deleteRolePerms(@Param("roleId") Long roleId);

    @Select("SELECT permission_id FROM sys_role_permission WHERE role_id = #{roleId}")
    List<Long> selectPermIdsByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT DISTINCT p.perm_code FROM sys_permission p " +
            "JOIN sys_role_permission rp ON rp.permission_id = p.id " +
            "JOIN sys_user_role ur ON ur.role_id = rp.role_id WHERE ur.user_id = #{userId}")
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);
}
