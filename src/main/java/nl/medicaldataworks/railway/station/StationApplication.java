package nl.medicaldataworks.railway.station;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@SpringBootApplication
public class StationApplication {

    public static void main(String[] args) throws InterruptedException, IOException {
        SpringApplication.run(StationApplication.class, args);

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://localhost:2375")
                .withDockerTlsVerify(false)
                .withDockerCertPath("C:\\Users\\user\\.docker\\certs")
                .withDockerConfig("C:\\Users\\user\\.docker")
                .withApiVersion("1.30") // optional
                .withRegistryUrl("https://registry.hub.docker.com/")
                .withRegistryUsername("user")
                .withRegistryPassword("password")
                .withRegistryEmail("email")
                .build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

        dockerClient.pullImageCmd("postgres:latest").exec(new PullImageResultCallback()).awaitCompletion();

        CreateContainerResponse container = dockerClient.createContainerCmd("postgres:latest")
                .withAttachStderr(true)
                .withAttachStdout(true)
                .exec();
        String containerId = container.getId();

        dockerClient.startContainerCmd(container.getId()).exec();

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd("ls")
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

        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback).awaitCompletion();

        dockerClient.stopContainerCmd(container.getId()).exec();

        System.out.println(stdout.toString());
    }

}
