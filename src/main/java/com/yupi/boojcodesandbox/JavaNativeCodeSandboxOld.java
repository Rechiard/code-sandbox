package com.yupi.boojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.WordTree;
import com.yupi.boojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.boojcodesandbox.model.ExecuteCodeResponse;
import com.yupi.boojcodesandbox.model.ExecuteMessage;
import com.yupi.boojcodesandbox.model.JudgeInfo;
import com.yupi.boojcodesandbox.utils.ProcessUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaNativeCodeSandboxOld implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 5000L;

    private static final String SECURITY_MANAGER_PATH = "F:\\IDEAProject\\booj-code-sandbox\\src\\main\\resources\\security";
    private static final String SECURITY_MANAGER_CLASS_NAME = "MySecurityManager";


    private static final List<String> blackList = Arrays.asList("Files","exec");

    private static final WordTree WORD_TREE;

    static {
        // 校验代码中是否携带危险代码（读文件、写文件、执行程序等）
        // WordTree是huTool的一个字典树，把需要判断的字符串存储在wordTree中，然后从code中检查是否有wordTree中的字符串
        // 初始化加载一次，没必要每次都重新添加到wordTree中，节省资源
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(blackList);
    }

    public static void main(String[] args) {
        JavaNativeCodeSandboxOld javaNativeCodeSandbox = new JavaNativeCodeSandboxOld();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
        String code = ResourceUtil.readStr("testCode/unsafeCode/RunFileError.java", StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/simpleCompute/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        System.out.println(javaNativeCodeSandbox.executeCode(executeCodeRequest));
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        //System.setSecurityManager(new DenySecurityManager());
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        // 黑白名单
//        FoundWord foundWord = WORD_TREE.matchWord(code);
//        if(foundWord != null){
//            System.out.println("！包含禁止词：" + foundWord.getFoundWord() + " !");
//            return null;
//        }

        // 1. 把用户的代码保存为文件
        String userDir = System.getProperty("user.dir");
        // 用File.separator是为了兼容不同的操作系统，自动配置 \ 斜杠符号
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码目录是否存在，没有就新建
        if(!FileUtil.exist(globalCodePathName)){
            FileUtil.mkdir(globalCodePathName);
        }

        // 把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

        // 2. 编译代码，得到 class 文件
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);
        } catch (Exception e) {
            return getErrorResponse(e);
        }

        // 3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for(String inputArgs : inputList){
            //String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeParentPath, SECURITY_MANAGER_PATH,SECURITY_MANAGER_CLASS_NAME, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);

                // 守护线程，用来监控程序执行的时间是否有人故意占用资源不释放（超时控制）
//                new Thread(() -> {
//                    try {
//                        Thread.sleep(TIME_OUT);
//                        System.out.println("超时了，中断");
//                        runProcess.destroy();
//                    } catch (InterruptedException e){
//                        throw new RuntimeException(e);
//                    }
//                }).start();

                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
//                ExecuteMessage executeMessage = ProcessUtils.runInterAndGetMessage(runProcess, "运行", inputArgs);
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                return getErrorResponse(e);
            }
        }

        // 4. 整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 取用最大值，便于判断是否超时
        // TODO 每个测试用例都有一个独立的内存、时间占用的统计
        long maxTime = 0;

        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(executeMessage.getErrorMessage())) {
                executeCodeResponse.setMessage(errorMessage);
                // 用户提交的代码执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            if(executeMessage.getTime() != null){
                maxTime = Math.max(maxTime, executeMessage.getTime());
            }
        }

        // 正常运行完成
        if(outputList.size() == executeMessageList.size()){
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);



        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        // 要借助第三方库来获取内存占用，非常麻烦
        // TODO 可以自己实现内存占用的统计
//        judgeInfo.setMemory();
        executeCodeResponse.setJudgeInfo(judgeInfo);

        // 5. 文件清除
        if(userCodeFile.getParentFile() != null){
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
        }

        return executeCodeResponse;
    }


    /**
     *  6. 错误处理， 提升程序健壮性
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e){
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
