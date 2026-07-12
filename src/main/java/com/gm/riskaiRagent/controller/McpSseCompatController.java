package com.gm.riskaiRagent.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class McpSseCompatController {

    // 兼容Cursor错误POST /sse的请求，重定向到GET /sse
    @PostMapping("/sse")
    public void postSseCompat(HttpServletResponse response){
        response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
    }
}
