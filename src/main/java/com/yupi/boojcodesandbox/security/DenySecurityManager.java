package com.yupi.boojcodesandbox.security;


import java.security.Permission;

/**
 * 禁止所有的安全管理器
 */
public class DenySecurityManager extends SecurityManager {

    /**
     * 检查所有的权限
     * @param perm   the requested permission.
     */
    @Override
    public void checkPermission(Permission perm) {
        throw new SecurityException("权限异常" + perm.getActions() + perm.toString());
    }
}
