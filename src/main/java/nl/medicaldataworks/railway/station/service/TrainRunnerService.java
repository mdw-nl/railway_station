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
import org.apache.commons.lang.SystemUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
@Slf4j
public class TrainRunnerService {
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
        return container.getId();
    }

    public void stopContainer (String id){
        dockerClient.stopContainerCmd(id).exec();
    }

    public void addInputToTrain(String InputJson) {
        //TODO write to input file
    }
    //TODO BROKEN
    public void executeCommand(String containerId) throws InterruptedException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        ExecCreateCmdResponse applicationResponse;
        applicationResponse = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withWorkingDir("/")
                .withPrivileged(true)
                .withCmd("/bin/sh", "runStation.sh")
                .exec();

        ExecStartResultCallback callback = new ExecStartResultCallback() {
            @Override
            public void onNext(Frame frame) {
                System.out.println("frame: " + frame);
                super.onNext(frame);
            }
        };

        //        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(
//                new ExecStartResultCallback(stdout, stderr)).awaitCompletion();
//
//        dockerClient.execStartCmd(applicationResponse.getId()).exec(callback).awaitCompletion();
//        log.info("Output from the container: {}", stdout);
    }
    //TODO BROKEN
    public void readOutputFile(String containerId) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        ExecCreateCmdResponse output = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withWorkingDir("/")
                .withPrivileged(true)
                .withCmd("cat", "output.txt")
                .exec();

        ExecStartResultCallback callback = new ExecStartResultCallback() {
            @Override
            public void onNext(Frame frame) {
                System.out.println("frame: " + frame);
                super.onNext(frame);
            }
        };
    }
}
