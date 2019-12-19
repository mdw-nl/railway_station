package nl.medicaldataworks.railway.station.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class TrainRunnerService {
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
        FileUtils.deleteDirectory(workingDir.resolve(id).toFile()); //TODO delete is not working
    }

    public String readOutputFromTrain(String containerId) throws IOException, InterruptedException {
        Path outputFile = workingDir.resolve(containerId).resolve("output.txt");
        String cmd = "docker cp " + containerId + ":/output.txt "  + outputFile.toString();
        java.lang.Runtime.getRuntime().exec(cmd).waitFor();
        return new String(Files.readAllBytes(outputFile));
    }

    public void addInputToTrain(String containerId, String inputJson) throws IOException, InterruptedException {
        Path inputFile = workingDir.resolve(containerId).resolve("input.txt");
        Files.write(inputFile, inputJson.getBytes());
        String cmd = "docker cp " + inputFile.toString() + " " + containerId + ":/input.txt";
        java.lang.Runtime.getRuntime().exec(cmd).waitFor();
    }

    public void executeCommand(String containerId) throws InterruptedException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withWorkingDir("/")
                .withPrivileged(true)
                .withCmd("/bin/sh", "runStation.sh")
                .exec();

        dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .exec(new ExecStartResultCallback(stdout, stderr)).awaitCompletion();
        log.info("Output from the container: {}", stdout);
    }
}
