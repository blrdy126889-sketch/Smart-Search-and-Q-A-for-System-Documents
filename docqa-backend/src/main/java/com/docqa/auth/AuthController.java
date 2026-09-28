package com.docqa.auth;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.docqa.common.api.R;
import com.docqa.common.exception.BizException;
import com.docqa.common.util.SecurityUtils;
import com.docqa.system.entity.SysUser;
import com.docqa.system.mapper.SysRoleMapper;
import com.docqa.system.mapper.SysUserMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口：登录 / 登出 / 当前用户 / 改密
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    @Data
    public static class LoginDTO {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class PasswordDTO {
        @NotBlank private String oldPassword;
        @NotBlank private String newPassword;
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody @Validated LoginDTO dto) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()).last("LIMIT 1"));
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BizException(403, "账号已被禁用，请联系管理员");
        }
        StpUtil.login(user.getId());
        var roleCodes = userMapper.selectRoleCodesByUserId(user.getId());
        var roleIds = userMapper.selectRolesByUserId(user.getId())
                .stream().map(r -> ((Number) r.get("id")).longValue()).toList();
        var permCodes = roleMapper.selectPermCodesByUserId(user.getId());
        StpUtil.getSession().set("username", user.getUsername());
        StpUtil.getSession().set("nickname", user.getNickname());
        StpUtil.getSession().set("roleCodes", roleCodes);
        StpUtil.getSession().set("roleIds", roleIds);

        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId()).set(SysUser::getLastLoginAt, OffsetDateTime.now()));

        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("roles", roleCodes);
        data.put("perms", permCodes);
        return R.ok(data);
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        SysUser user = userMapper.selectById(SecurityUtils.userId());
        if (user == null) throw new BizException(401, "用户不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("email", user.getEmail());
        data.put("roles", SecurityUtils.roleCodes());
        data.put("perms", roleMapper.selectPermCodesByUserId(user.getId()));
        return R.ok(data);
    }

    @PutMapping("/password")
    public R<Void> changePassword(@RequestBody @Validated PasswordDTO dto) {
        SysUser user = userMapper.selectById(SecurityUtils.userId());
        if (user == null || !BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BizException("原密码错误");
        }
        if (dto.getNewPassword().length() < 6) {
            throw new BizException("新密码长度不能少于 6 位");
        }
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getPassword, BCrypt.hashpw(dto.getNewPassword())));
        StpUtil.logout();
        return R.ok();
    }
}
