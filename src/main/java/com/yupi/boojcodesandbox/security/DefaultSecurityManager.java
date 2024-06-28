package com.yupi.boojcodesandbox.security;


import java.security.Permission;

/**
 * 默认安全管理器
 */
public class DefaultSecurityManager extends SecurityManager {

    /**
     * 检查所有的权限
     * @param perm   the requested permission.
     */
    @Override
    public void checkPermission(Permission perm) {
        System.out.println(perm);
        System.out.println("默认不做任何限制");
        super.checkPermission(perm);
    }

    @Override
    public void checkExec(String cmd) {
        super.checkExec(cmd);
    }

    @Override
    public void checkRead(String file) {
        super.checkRead(file);
    }

    @Override
    public void checkWrite(String file) {
        super.checkWrite(file);
    }

    @Override
    public void checkDelete(String file) {
        super.checkDelete(file);
    }

    @Override
    public void checkConnect(String host, int port) {
        super.checkConnect(host, port);
    }
}
