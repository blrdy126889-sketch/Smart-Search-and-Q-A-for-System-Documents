package com.docqa.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docqa.common.api.R;
import com.docqa.common.exception.BizException;
import com.docqa.common.log.OpLog;
import com.docqa.common.util.SecurityUtils;
import com.docqa.system.entity.SysPermission;
import com.docqa.system.entity.SysRole;
import com.docqa.system.mapper.SysPermissionMapper;
import com.docqa.system.mapper.SysRoleMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色与权限管理（ADMIN）
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;

    @GetMapping("/roles")
    public R<List<Map<String, Object>>> roles() {
        SecurityUtils.checkPerm("system:role:list");
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysRole role : roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSort))) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", role.getId());
            m.put("roleName", role.getRoleName());
            m.put("roleCode", role.getRoleCode());
            m.put("roleDesc", role.getRoleDesc());
            m.put("permIds", roleMapper.selectPermIdsByRoleId(role.getId()));
            result.add(m);
        }
        return R.ok(result);
    }

    @Data
    public static class RoleDTO {
        private String roleName;
        private String roleCode;
        private String roleDesc;
    }

    @PostMapping("/roles")
    @OpLog(module = "系统管理", operation = "新增角色")
    public R<Void> create(@RequestBody RoleDTO dto) {
        SecurityUtils.checkPerm("system:role:list");
        if (dto.getRoleCode() == null || dto.getRoleCode().isBlank()) throw new BizException("角色编码不能为空");
        Long exists = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, dto.getRoleCode()));
        if (exists > 0) throw new BizException("角色编码已存在");
        SysRole role = new SysRole();
        role.setRoleName(dto.getRoleName());
        role.setRoleCode(dto.getRoleCode());
        role.setRoleDesc(dto.getRoleDesc());
        roleMapper.insert(role);
        return R.ok();
    }

    @PutMapping("/roles/{id}")
    @OpLog(module = "系统管理", operation = "修改角色")
    public R<Void> update(@PathVariable Long id, @RequestBody RoleDTO dto) {
        SecurityUtils.checkPerm("system:role:list");
        SysRole role = roleMapper.selectById(id);
        if (role == null) throw new BizException("角色不存在");
        role.setRoleName(dto.getRoleName());
        role.setRoleDesc(dto.getRoleDesc());
        roleMapper.updateById(role);
        return R.ok();
    }

    @DeleteMapping("/roles/{id}")
    @OpLog(module = "系统管理", operation = "删除角色")
    public R<Void> delete(@PathVariable Long id) {
        SecurityUtils.checkPerm("system:role:list");
        if (id == 1L) throw new BizException("内置管理员角色不可删除");
        roleMapper.deleteById(id);
        roleMapper.deleteRolePerms(id);
        return R.ok();
    }

    @PutMapping("/roles/{id}/permissions")
    @OpLog(module = "系统管理", operation = "角色授权")
    @Transactional
    public R<Void> assignPerms(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SecurityUtils.checkPerm("system:role:list");
        roleMapper.deleteRolePerms(id);
        @SuppressWarnings("unchecked")
        List<Number> permIds = (List<Number>) body.getOrDefault("permIds", List.of());
        for (Number pid : permIds) {
            roleMapper.insertRolePerm(id, pid.longValue());
        }
        return R.ok();
    }

    @GetMapping("/permissions/tree")
    public R<List<Map<String, Object>>> permTree() {
        List<SysPermission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSort));
        return R.ok(buildTree(all, 0L));
    }

    private List<Map<String, Object>> buildTree(List<SysPermission> all, Long parentId) {
        List<Map<String, Object>> tree = new ArrayList<>();
        for (SysPermission p : all) {
            if (parentId.equals(p.getParentId())) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", p.getId());
                node.put("permName", p.getPermName());
                node.put("permCode", p.getPermCode());
                node.put("permType", p.getPermType());
                node.put("children", buildTree(all, p.getId()));
                tree.add(node);
            }
        }
        return tree;
    }
}
