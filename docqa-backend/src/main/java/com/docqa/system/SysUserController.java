package com.docqa.system;

import cn.dev33.satoken.secure.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.docqa.common.api.PageResult;
import com.docqa.common.api.R;
import com.docqa.common.exception.BizException;
import com.docqa.common.log.OpLog;
import com.docqa.common.util.SecurityUtils;
import com.docqa.system.entity.SysUser;
import com.docqa.system.mapper.SysUserMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理（ADMIN）
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class SysUserController {

    private final SysUserMapper userMapper;

    @GetMapping
    public R<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        SecurityUtils.checkPerm("system:user:list");
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysUser>()
                        .and(keyword != null && !keyword.isBlank(), w ->
                                w.like(SysUser::getUsername, keyword).or().like(SysUser::getNickname, keyword))
                        .orderByDesc(SysUser::getId));
        List<Map<String, Object>> records = p.getRecords().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("email", u.getEmail());
            m.put("status", u.getStatus());
            m.put("lastLoginAt", u.getLastLoginAt());
            var roles = userMapper.selectRolesByUserId(u.getId());
            m.put("roles", roles.stream().map(r -> r.get("roleCode")).toList());
            m.put("roleIds", roles.stream().map(r -> ((Number) r.get("id")).longValue()).toList());
            return m;
        }).toList();
        return R.ok(PageResult.of(records, p.getTotal(), page, size));
    }

    @Data
    public static class UserDTO {
        @NotBlank private String username;
        @NotBlank private String nickname;
        @NotBlank private String password;
        private String email;
    }

    @PostMapping
    @OpLog(module = "系统管理", operation = "新增用户")
    public R<Void> create(@RequestBody @Validated UserDTO dto) {
        SecurityUtils.checkPerm("system:user:add");
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (exists > 0) throw new BizException("用户名已存在");
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setStatus(1);
        userMapper.insert(user);
        userMapper.insertUserRole(user.getId(), 4L);
        return R.ok();
    }

    @PutMapping("/{id}")
    @OpLog(module = "系统管理", operation = "修改用户")
    public R<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SecurityUtils.checkPerm("system:user:add");
        var wrapper = new LambdaUpdateWrapper<SysUser>().eq(SysUser::getId, id);
        if (body.containsKey("nickname")) wrapper.set(SysUser::getNickname, body.get("nickname"));
        if (body.containsKey("email")) wrapper.set(SysUser::getEmail, body.get("email"));
        if (body.containsKey("status")) wrapper.set(SysUser::getStatus, ((Number) body.get("status")).intValue());
        userMapper.update(null, wrapper);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @OpLog(module = "系统管理", operation = "删除用户")
    public R<Void> delete(@PathVariable Long id) {
        SecurityUtils.checkPerm("system:user:add");
        if (id.equals(SecurityUtils.userId())) throw new BizException("不能删除当前登录账号");
        userMapper.deleteById(id);
        userMapper.deleteUserRoles(id);
        return R.ok();
    }

    @PostMapping("/{id}/roles")
    @OpLog(module = "系统管理", operation = "分配角色")
    @Transactional
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SecurityUtils.checkPerm("system:user:add");
        userMapper.deleteUserRoles(id);
        @SuppressWarnings("unchecked")
        List<Number> roleIds = (List<Number>) body.getOrDefault("roleIds", List.of());
        for (Number roleId : roleIds) {
            userMapper.insertUserRole(id, roleId.longValue());
        }
        return R.ok();
    }
}
