import java.security.Permission;

public class MySecurityManager extends SecurityManager {

    @Override
    public void checkPermission(Permission perm) {

    }

    /**
     * 检测程序是否可执行
     * @param cmd   the specified system command.
     */
    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("checkExec 权限异常：" +cmd);
    }

    /**
     * 检测程序是否允许读文件
     * @param file   the system-dependent file name.
     */
    @Override
    public void checkRead(String file) {
        //throw new SecurityException("checkRead 权限异常：" +file);
    }

    /**
     * 检测程序是否允许写文件
     * @param file   the system-dependent filename.
     */
    @Override
    public void checkWrite(String file) {
        //throw new SecurityException("checkWrite 权限异常：" +file);
    }

    /**
     * 检测程序是否允许
     * @param file   the system-dependent filename.
     */
    @Override
    public void checkDelete(String file) {
        //throw new SecurityException("checkDelete 权限异常：" +file);
    }

    /**
     * 检查程序是否允许连接网络
     * @param host   the host name port to connect to.
     * @param port   the protocol port to connect to.
     */
    @Override
    public void checkConnect(String host, int port) {
        //throw new SecurityException("checkConnect 权限异常：" + host + ":" + port);
    }
}
