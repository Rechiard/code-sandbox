package com.yupi.boojcodesandbox.controller;


import com.yupi.boojcodesandbox.JavaDockerCodeSandbox;
import com.yupi.boojcodesandbox.JavaNativeCodeSandbox;
import com.yupi.boojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.boojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController("/")
public class MainController {

    // 定义鉴权请求头和秘钥
    public static final String AUTH_REQUEST_HEADER = "auth";

    public static final String AUTH_REQUEST_SECRET = "secretKey";

    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

    @Resource
    private JavaDockerCodeSandbox javaDockerCodeSandbox;

    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    @PostMapping("/executeCode")
    public ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request, HttpServletResponse response) {
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        // 基本的认证
        if(!AUTH_REQUEST_SECRET.equals(authHeader)){
            response.setStatus(403);
            return null;
        }
        if(executeCodeRequest == null){
            throw new RuntimeException("请求参数为空");
        }
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        return executeCodeResponse;
    }

    @PostMapping("/executeCodeByDocker")
    public ExecuteCodeResponse executeCodeByDocker(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request, HttpServletResponse response) {
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        // 基本的认证
        if(!AUTH_REQUEST_SECRET.equals(authHeader)){
            response.setStatus(403);
            return null;
        }
        if(executeCodeRequest == null){
            throw new RuntimeException("请求参数为空");
        }
        ExecuteCodeResponse executeCodeResponse = javaDockerCodeSandbox.executeCode(executeCodeRequest);
        return executeCodeResponse;
    }
}
