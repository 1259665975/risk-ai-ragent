package com.gm.riskaiRagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gm.riskaiRagent.common.Result;
import com.gm.riskaiRagent.common.ResultCode;
import com.gm.riskaiRagent.security.AuthContext;
import com.gm.riskaiRagent.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * Token 鉴权拦截器，解析 Authorization 头并把用户写入 AuthContext。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    @Override
    /**
     * 请求进入控制器前完成 Token 解析、登录校验和管理员权限校验。
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = extractToken(request);
        AuthUser authUser = tokenService.resolve(token);
        if (authUser == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    Result.error(ResultCode.UNAUTHORIZED.getCode(), "未登录或登录已过期"));
            return false;
        }

        String uri = request.getRequestURI();
        if (uri.startsWith("/api/admin") && !authUser.isAdmin()) {
            boolean readOnlyCategory = "GET".equalsIgnoreCase(request.getMethod())
                    && "/api/admin/categories".equals(uri);
            if (!readOnlyCategory) {
                writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                        Result.error(ResultCode.FORBIDDEN.getCode(), "无权限访问"));
                return false;
            }
        }

        AuthContext.set(authUser);
        return true;
    }

    @Override
    /**
     * 请求结束后清理 ThreadLocal，避免线程复用时串用用户身份。
     */
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuthContext.clear();
    }

    /**
     * 从 Authorization: Bearer xxx 请求头中提取 Token。
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    /**
     * 拦截器内直接写统一 JSON 响应，避免未登录请求继续进入控制器。
     */
    private void writeJson(HttpServletResponse response, int status, Result<?> body) throws Exception {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
