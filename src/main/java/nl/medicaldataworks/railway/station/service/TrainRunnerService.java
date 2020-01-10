package nl.medicaldataworks.railway.station.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import lombok.extern.slf4j.Slf4j;
import nl.medicaldataworks.railway.station.web.dto.TaskDto;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TrainRunnerService {
    public static final String RUN_MASTER = "runMaster.sh";
    public static final String RUN_STATION = "runStation.sh";
    private DockerClient dockerClient;
    private Path workingDir = new File("./").toPath();

    public TrainRunnerService(){
        dockerClient = createDockerClient();
    }

    private DockerClient createDockerClient(){
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(determineDockerHost())
                .withDockerTlsVerify(false)
                .withDockerCertPath("~/.docker/certs")
                .withDockerConfig("~/.docker")
                .withApiVersion("1.30")
                .build();
        return DockerClientBuilder.getInstance(config).build();
    }

    private String determineDockerHost(){
        if (SystemUtils.IS_OS_WINDOWS) {
            return "tcp://localhost:2375";
        } else {
            return "unix:///var/run/docker.sock";
        }
    }

    public String startContainer (String dockerUrl) throws InterruptedException {
        dockerClient.pullImageCmd(dockerUrl).exec(new PullImageResultCallback()).awaitCompletion();
        CreateContainerResponse container = dockerClient.createContainerCmd(dockerUrl)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .exec();
        dockerClient.startContainerCmd(container.getId()).exec();
        workingDir.resolve(container.getId()).toFile().mkdir();
        return container.getId();
    }

    public void stopContainer (String id) throws IOException {
        dockerClient.stopContainerCmd(id).exec();
        FileUtils.deleteDirectory(workingDir.resolve(id).toFile());
    }

    public String readOutputFromTrain(String containerId) throws IOException, InterruptedException {
        Path outputFile = workingDir.resolve(containerId).resolve("output.txt");
        String cmd = "docker cp " + containerId + ":/output.txt "  + outputFile.toString();
        java.lang.Runtime.getRuntime().exec(cmd).waitFor();
        return new String(Files.readAllBytes(outputFile));
    }

    public List<TaskDto> readTaskListFromTrain(String containerId) throws IOException, InterruptedException {
        Path outputFile = workingDir.resolve(containerId).resolve("tasks.json");
        String cmd = "docker cp " + containerId + ":/tasks.json "  + outputFile.toString();
        java.lang.Runtime.getRuntime().exec(cmd).waitFor();
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = new String(Files.readAllBytes(outputFile));
        List<TaskDto> taskDtos = new ArrayList<>();
        if (!jsonString.isEmpty()){
            taskDtos = objectMapper.readValue(jsonString, new TypeReference<List<TaskDto>>(){});
        }
        return taskDtos;
    }

    public void addInputToTrain(String containerId, String input) throws IOException, InterruptedException {
//        if (input == null){
//            return;
//        }

        Path inputFile = workingDir.resolve(containerId).resolve("input.txt");
        Files.write(inputFile, input.getBytes());
        String cmd = "docker cp " + inputFile.toString() + " " + containerId + ":/input.txt";
        java.lang.Runtime.getRuntime().exec(cmd).waitFor();
    }

    public void executeCommand(String containerId, boolean master) throws InterruptedException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        String command = RUN_STATION;
        if(master){
            command = RUN_MASTER;
        }
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withWorkingDir("/")
                .withPrivileged(true)
                .withCmd("/bin/sh", command)
                .exec();

        dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .exec(new ExecStartResultCallback(stdout, stderr)).awaitCompletion();
        log.info("Output from the container: {}", stdout);
    }
}
