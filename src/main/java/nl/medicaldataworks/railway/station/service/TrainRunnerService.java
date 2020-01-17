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
import org.apache.commons.lang.SystemUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TrainRunnerService {
    public static final String RUN_MASTER = "runMaster.sh";
    public static final String RUN_STATION = "runStation.sh";
    public static final String INPUT_FILE = "input.txt";
    public static final String COMPLETED_TASKS_FILE = "completed-client-tasks.json";
    public static final String OUTPUT_FILE = "output.txt";
    public static final String NEW_TASKS_FILE = "new-client-tasks.json";
    private static final Path DOCKER_DIR = new File("/opt").toPath();
    private Path workingDir = new File("./").toPath();

    private DockerClient dockerClient;

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
//        FileUtils.deleteDirectory(workingDir.resolve(id).toFile());
    }

    public void addInputToTrain(String containerId, String input) throws IOException, InterruptedException {
        Path inputFile = workingDir.resolve(containerId).resolve(INPUT_FILE);
        Files.write(inputFile, input.getBytes());
        String cmd = "docker cp " + inputFile.toString() + " " + containerId + ":" + DOCKER_DIR.resolve(INPUT_FILE).toString();
        java.lang.Runtime.getRuntime().exec(cmd).waitFor();
    }

    public void addCompletedTasksToTrain(String containerId, List<TaskDto> taskDtos) throws IOException, InterruptedException{
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(taskDtos);
        Path completedTasksFile = workingDir.resolve(containerId).resolve(COMPLETED_TASKS_FILE);
        Files.write(completedTasksFile, jsonString.getBytes());
        String cmd = "docker cp " + completedTasksFile.toString() + " " + containerId + ":" + DOCKER_DIR.resolve(COMPLETED_TASKS_FILE).toString();
        java.lang.Runtime.getRuntime().exec(cmd).waitFor();
    }

    public String readOutputFromTrain(String containerId) throws IOException, InterruptedException {
        Path outputFile = workingDir.resolve(containerId).resolve(OUTPUT_FILE);
        String cmd = "docker cp " + containerId + ":" + DOCKER_DIR.resolve(OUTPUT_FILE).toString() + " "  + outputFile.toString();
        java.lang.Runtime.getRuntime().exec(cmd).waitFor();
        return new String(Files.readAllBytes(outputFile));
    }

    public List<TaskDto> parseNewTasksFromTrain(String containerId) throws IOException, InterruptedException {
        Path newTasksFile = workingDir.resolve(containerId).resolve(NEW_TASKS_FILE);
        String cmd = "docker cp " + containerId + ":" + DOCKER_DIR.resolve(NEW_TASKS_FILE).toString() + " "  + newTasksFile.toString();
        java.lang.Runtime.getRuntime().exec(cmd).waitFor();
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = new String(Files.readAllBytes(newTasksFile));
        List<TaskDto> taskDtos = new ArrayList<>();
        if (!jsonString.isEmpty()){
            taskDtos = objectMapper.readValue(jsonString, new TypeReference<List<TaskDto>>(){});
        }
        return taskDtos;
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
