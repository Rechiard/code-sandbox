 package com.yupi.boojcodesandbox;

 import com.yupi.boojcodesandbox.model.ExecuteCodeRequest;
 import com.yupi.boojcodesandbox.model.ExecuteCodeResponse;

 /**
 * 代码沙箱接口定义，提高通用性
 * 之后在项目代码中只调用接口，不调用具体的实现类，这样就可以在使用其他的代码沙箱实现类时，不用修改名称，便于扩展
 */
public interface CodeSandbox {

    // TODO 可以提供一个查看代码沙箱状态的接口

    /**
     * 执行代码
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
