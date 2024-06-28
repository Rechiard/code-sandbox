package com.yupi.boojcodesandbox.security;

import cn.hutool.core.io.FileUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class TestSecurityManager {
    public static void main(String[] args) {
        System.setSecurityManager(new MySecurityManager());
        List<String> strings = FileUtil.readLines("F:\\IDEAProject\\booj-code-sandbox\\src\\main\\java\\com\\yupi\\boojcodesandbox\\security", StandardCharsets.UTF_8);
    }
}
