package com.yupi.boojcodesandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.yupi.boojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.boojcodesandbox.model.ExecuteCodeResponse;
import com.yupi.boojcodesandbox.model.ExecuteMessage;
import com.yupi.boojcodesandbox.model.JudgeInfo;
import com.yupi.boojcodesandbox.utils.ProcessUtils;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Java Docker代码沙箱模板方法的实现
 */
@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {

    private static final long TIME_OUT = 5000L;

    private static final Boolean FIRST_INIT = true;


    public static void main(String[] args) {
        JavaDockerCodeSandbox javaNativeCodeSandbox = new JavaDockerCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
//        String code = ResourceUtil.readStr("testCode/unsafeCode/RunFileError.java", StandardCharsets.UTF_8);
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/simpleCompute/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        System.out.println(javaNativeCodeSandbox.executeCode(executeCodeRequest));
    }

    /**
     * 创建容器，把文件复制到容器内并且执行，得到返回结果
     * @param inputList
     * @return
     */
    @Override
    public List<ExecuteMessage> runFile(List<String> inputList) {
        String userCodeParentPath = super.userCodeParentPath;
        // 获取默认的 Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String image = "openjdk:8-alpine";
        if(FIRST_INIT){
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item);
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
        }
        System.out.println("下载完成");

        // 创建容器，把文件复制到容器内
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);

        HostConfig hostConfig = new HostConfig();
        // 创建容器时，指定文件路径（Volume）映射，作用是把本地文件同步到容器中，可以让容器访问。也叫容器挂载目录
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        // 设置内存限制，防止别人写死程序
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        // seccomp是linux内核的一个安全子系统，用于限制由应用程序的系统调用，防止别人读写文件
//        hostConfig.withSecurityOpts(Arrays.asList("seccomp = "));
        // withAttachStderr withAttachStdin withAttachStdout将docker和服务器进行连接，可以得到docker的输入和输出
        //利用withTty开启守护进程，利用守护进程的方式使docker容器一直运行，方便交互
        CreateContainerResponse createConfigResponse = containerCmd
                // 设置网络关闭，防止别人刷带宽
                .withNetworkDisabled(true)
                .withHostConfig(hostConfig)
                .withReadonlyRootfs(true)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();
        System.out.println(createConfigResponse);
        String containerId = createConfigResponse.getId();

        // 启动容器,执行代码
        dockerClient.startContainerCmd(containerId).exec();

        // 执行docker附带java命令：docker exec container_name java -cp /app Main 1 3
        // 执行命令并获取结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for(String inputArgs : inputList){
            StopWatch stopWatch = new StopWatch();
            String[] inputArgsArray = inputArgs.split(" ");
            // huTool工具类，在字符串数组后面追加字符串数组
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("创建执行命令：" + execCreateCmdResponse);

            ExecuteMessage executeMessage = new ExecuteMessage();
            final String[] message = {null};
            final String[] errorMessage = {null};
            long time = 0L;
            final boolean[] timeout = {true};
            String execId = execCreateCmdResponse.getId();
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback(){
                @Override
                public void onComplete() {
                    // 如果执行完成，则表示没有超时
                    timeout[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    if(frame.getStreamType().equals(StreamType.STDERR)){
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果：" + errorMessage[0]);
                    }else{
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果：" + message[0]);
                    }
                    super.onNext(frame);
                }
            };

            // 获取占用内存
            final long[] maxMemory = {0L};

            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(maxMemory[0], statistics.getMemoryStats().getUsage());
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

                @Override
                public void close() throws IOException {

                }
            });
            statsCmd.exec(statisticsResultCallback);

            // awaitCompletion方法是阻塞程序执行，因为docker执行命令需要时间，不阻塞就会出现多线程问题
            try {
                stopWatch.start();
                // 超时就不执行直接往下走
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
                statsCmd.close();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
        }
        return executeMessageList;
    }
}
