package com.campus.lab.util;

import com.campus.lab.common.BizException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 从请求属性中读取 JWT 拦截器注入的当前用户信息。
 */
public final class Auths {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_STUDENT = "STUDENT";

    private Auths() {
    }

    public static Long userId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    public static String role(HttpServletRequest request) {
        return (String) request.getAttribute("role");
    }

    public static String userName(HttpServletRequest request) {
        return (String) request.getAttribute("userName");
    }

    public static void requireAdmin(HttpServletRequest request) {
        if (!ROLE_ADMIN.equals(role(request))) {
            throw new BizException("无权限：该操作需要管理员角色");
        }
    }
}
