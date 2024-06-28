package com.yupi.boojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.boojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.boojcodesandbox.model.ExecuteCodeResponse;
import com.yupi.boojcodesandbox.model.ExecuteMessage;
import com.yupi.boojcodesandbox.model.JudgeInfo;
import com.yupi.boojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox{



    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 5000L;


    public static String userDir;

    public static String globalCodePathName;
    public static String userCodeParentPath;

    public static String userCodePath;

    static {
        userDir = System.getProperty("user.dir");
        // 用File.separator是为了兼容不同的操作系统，自动配置 \ 斜杠符号
        globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;


        userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        // 把用户的代码隔离存放
        userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
    }


    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        // 1.把用户的代码保存为文件
        File userCodeFile = saveCodeToFile(code);

        // 2.编译代码，得到 class 文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        System.out.println(compileFileExecuteMessage);

        // 3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = runFile(inputList);

        // 4. 整理输出结果
        ExecuteCodeResponse executeCodeResponse = getOutputResponse(executeMessageList);

        // 5. 文件清除
        if(!deleteFile(userCodeFile)){
            log.error("删除失败，userCodeFilePath = {}", userCodeFile.getAbsolutePath());
        }

        return executeCodeResponse;
    }


    /**
     * 1.把用户的代码保存为文件
     * @param code 用户代码
     * @return
     */
    public File saveCodeToFile(String code){
        // 判断全局代码目录是否存在，没有就新建
        if(!FileUtil.exist(globalCodePathName)){
            FileUtil.mkdir(globalCodePathName);
        }

        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 2. 编译代码，得到 class 文件
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile){

        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            if(executeMessage.getExitValue() != 0){
                throw new RuntimeException("编译错误");
            }
            return executeMessage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 3.执行文件，获得执行列表
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runFile(List<String> inputList){
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for(String inputArgs : inputList){
            //String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException("执行错误", e);
            }
        }
        return executeMessageList;
    }

    /**
     * 获取输出结果
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList){
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
        return executeCodeResponse;
    }

    /**
     * 5.删除文件
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile){
        if(userCodeFile.getParentFile() != null){
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
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
