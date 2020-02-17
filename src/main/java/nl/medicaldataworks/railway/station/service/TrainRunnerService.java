package nl.medicaldataworks.railway.station.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import lombok.extern.slf4j.Slf4j;
import nl.medicaldataworks.railway.station.config.StationConfiguration;
import nl.medicaldataworks.railway.station.web.dto.TaskDto;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TrainRunnerService {
    public static final String RUN_MASTER = "runMaster.sh";
    public static final String RUN_STATION = "runStation.sh";
    public static final String INPUT_FILE = "input.txt";
    public static final String COMPLETED_TASKS_FILE = "completed-client-tasks.json";
    public static final String LOG_FILE = "train.log";
    public static final String ERROR_LOG_FILE = "error.log";
    public static final String OUTPUT_FILE = "output.txt";
    public static final String NEW_TASKS_FILE = "new-client-tasks.json";
    private static final Path DOCKER_DIR = new File("/opt").toPath();
    public static final String NETWORK_NAME = "station";
    public static final String DOCKER_CERT_PATH = "~/.docker/certs";
    public static final String DOCKER_CONFIG = "~/.docker";
    public static final String API_VERSION = "1.30";
    public static final String DOCKER_PATH_WINDOWS = "tcp://localhost:2375";
    public static final String DOCKER_PATH_UNIX = "unix:///var/run/docker.sock";
    private Path workingDir = new File("./").toPath();

    private StationConfiguration stationConfiguration;
    private DockerClient dockerClient;

    public TrainRunnerService(StationConfiguration stationConfiguration){
        this.stationConfiguration = stationConfiguration;
        dockerClient = createDockerClient();
    }

    private DockerClient createDockerClient(){
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(determineDockerHost())
                .withDockerTlsVerify(false)
                .withDockerCertPath(DOCKER_CERT_PATH)
                .withDockerConfig(DOCKER_CONFIG)
                .withApiVersion(API_VERSION)
                .build();
        return DockerClientBuilder.getInstance(config).build();
    }

    private String determineDockerHost(){
        if (SystemUtils.IS_OS_WINDOWS) {
            return DOCKER_PATH_WINDOWS;
        } else {
            return DOCKER_PATH_UNIX;
        }
    }

    public String startContainer (String dockerUrl) throws Exception {
        dockerClient.pullImageCmd(dockerUrl).exec(new PullImageResultCallback()).awaitCompletion();
        List<String> environmentVariables = getEnvironmentVariables();
        CreateContainerResponse container = dockerClient.createContainerCmd(dockerUrl)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withEnv(environmentVariables)
                .withHostConfig(HostConfig
                        .newHostConfig()
                        .withNetworkMode(NETWORK_NAME)
                        .withAutoRemove(true))
                .exec();
        dockerClient.startContainerCmd(container.getId()).exec();
        workingDir.resolve(container.getId()).toFile().mkdir();

        return container.getId();
    }

    private List<String> getEnvironmentVariables() {
        List<String> environmentVariables = new ArrayList<>();
        for(Map.Entry<String, String> environmentVariable : stationConfiguration.getEnvironmentVariables().entrySet()){
            String environmentVariableString = String.format("%s=%s", environmentVariable.getKey(),
                                                                      environmentVariable.getValue());
            environmentVariables.add(environmentVariableString);
        }
        return environmentVariables;
    }

    public void stopContainer (String id) throws IOException {
        dockerClient.stopContainerCmd(id).exec();
        if (!stationConfiguration.getEnableAudit()) {
            FileUtils.deleteDirectory(workingDir.resolve(id).toFile());
        }
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

    public String parseAppLogsFromTrain(String containerId) throws IOException, InterruptedException {
        Path logFile = workingDir.resolve(containerId).resolve(LOG_FILE);
        String cmd = "docker cp " + containerId + ":" + DOCKER_DIR.resolve(LOG_FILE).toString() + " "  + logFile.toString();
        java.lang.Runtime.getRuntime().exec(cmd).waitFor();
        return logFile.toAbsolutePath().toString();
    }

    public void parseErrorLogsFromTrain(String containerId) throws Exception {
        Path logFile = workingDir.resolve(containerId).resolve(ERROR_LOG_FILE);
        String cmd = "docker cp " + containerId + ":" + DOCKER_DIR.resolve(ERROR_LOG_FILE).toString() + " "  + logFile.toString();
        java.lang.Runtime.getRuntime().exec(cmd).waitFor();
        FileInputStream fileInputStream = new FileInputStream(logFile.toFile());
        String errorLog = IOUtils.toString(fileInputStream, StandardCharsets.UTF_8.name());
        if (!errorLog.isEmpty()){
            throw(new Exception(errorLog));
        }
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
    }
}
