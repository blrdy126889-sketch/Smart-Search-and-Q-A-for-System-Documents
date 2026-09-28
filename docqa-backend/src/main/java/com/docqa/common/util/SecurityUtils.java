package com.docqa.common.util;

import cn.dev33.satoken.stp.StpUtil;
import com.docqa.common.exception.BizException;

import java.util.List;

/**
 * 当前登录用户工具
 */
public class SecurityUtils {

    public static long userId() {
        return StpUtil.getLoginIdAsLong();
    }

    public static String username() {
        Object u = StpUtil.getSession().get("username");
        return u == null ? "" : String.valueOf(u);
    }

    public static String nickname() {
        Object n = StpUtil.getSession().get("nickname");
        return n == null ? "" : String.valueOf(n);
    }

    @SuppressWarnings("unchecked")
    public static List<Long> roleIds() {
        Object r = StpUtil.getSession().get("roleIds");
        if (r instanceof List<?> list) {
            return (List<Long>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public static List<String> roleCodes() {
        Object r = StpUtil.getSession().get("roleCodes");
        if (r instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    /** 校验操作权限（ADMIN 直通，其余走 Sa-Token 权限码） */
    public static void checkPerm(String permCode) {
        if (roleCodes().contains("ADMIN")) return;
        StpUtil.checkPermission(permCode);
    }

    public static void assertLoggedIn() {
        if (!StpUtil.isLogin()) throw new BizException(401, "未登录");
    }
}
