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
import nl.medicaldataworks.railway.station.config.CentralConfiguration;
import nl.medicaldataworks.railway.station.web.dto.TaskDto;
import nl.medicaldataworks.railway.station.web.dto.TrainDto;
import org.apache.commons.lang.SystemUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;

import static org.apache.http.HttpVersion.HTTP;

@Service
@Slf4j
public class TrainRunnerService {
    public static final String TASK_API_PATH =  "/api/tasks";
    private WebClient webClient;
    private CentralConfiguration centralConfig;

    public TrainRunnerService(WebClient webClient, CentralConfiguration centralConfig) {
        this.webClient = webClient;
        this.centralConfig = centralConfig;
    }

    @Scheduled(fixedDelay = 10000)
    public void getNextTaskFromServer() throws URISyntaxException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(HTTP);
        builder.setHost(centralConfig.getHostname());
        builder.setPort(centralConfig.getPort());
        builder.setPath(TASK_API_PATH);
        builder.setParameter("station-id", "maastro");
        builder.setParameter("page", "0");
        builder.setParameter("sort", "creationTimestamp");
        builder.setParameter("creationTimestamp.dir", "desc");
        Mono<TaskDto> body = webClient
                .get()
                .uri(builder.build().toString())
                .retrieve()
                .bodyToMono(TaskDto.class); //TODO: list of taskdto, if null etc
        body.subscribe(response -> processTask(response));
    }

    private void processTask(TaskDto taskDto) {
        TrainDto trainDto = getTrain(taskDto.getTrain());
        log.info("Running task: {} for train: {}.", taskDto.getId(), trainDto.getId());
        runTrain();
    }

    private TrainDto getTrain(Long id) {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(HTTP);
        builder.setHost(centralConfig.getHostname());
        builder.setPort(centralConfig.getPort());
        builder.setPath(String.format("/api/trains/%s", id));
        try {
            return webClient
                    .get()
                    .uri(builder.build().toString())
                    .retrieve()
                    .bodyToMono(TrainDto.class)
                    .block();
        } catch (URISyntaxException e) {
            log.error("Unable to query central API for train.", e);
        }
        return null; //TODO: better error handling.
    }

    public void runTrain(){
        DockerClient dockerClient = createDockerClient();
        //TODO add master or client switch
        String id = new String();
        try {
            id = startContainer(dockerClient, "registry.gitlab.com/medicaldataworks/railway/prototypetrain:master");
        } catch (Exception e) {
            log.error("Could not start container: {}", e);
            return;
        }
        try {
            executeCommand(dockerClient, id);
        }
        catch (Exception e) {
            log.error("Could not execute container: {}", e);
        }
        finally {
            dockerClient.stopContainerCmd(id).exec();
        }

    }

    public DockerClient createDockerClient(){
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(determineDockerHost())
                .withDockerTlsVerify(false)
                .withDockerCertPath("~/.docker/certs")
                .withDockerConfig("~/.docker")
                .withApiVersion("1.30")
                .build();
        return DockerClientBuilder.getInstance(config).build();
    }

    public String determineDockerHost(){
        if (SystemUtils.IS_OS_WINDOWS) {
            return "tcp://localhost:2375";
        } else {
            return "unix:///var/run/docker.sock";
        }
    }

    public String startContainer (DockerClient dockerClient, String dockerUrl) throws InterruptedException {
        dockerClient.pullImageCmd(dockerUrl).exec(new PullImageResultCallback()).awaitCompletion();
        CreateContainerResponse container = dockerClient.createContainerCmd(dockerUrl)
            .withAttachStderr(true)
            .withAttachStdout(true)
            .exec();
        dockerClient.startContainerCmd(container.getId()).exec();
        return container.getId();
    }

    public void executeCommand(DockerClient dockerClient, String containerId) throws InterruptedException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withWorkingDir("/")
                .withPrivileged(true)
                .withCmd("/bin/sh", "runStation.sh")
                .exec();

        ExecCreateCmdResponse execCreateCmdResponse3 = dockerClient.execCreateCmd(containerId)
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

        //        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(
//                new ExecStartResultCallback(stdout, stderr)).awaitCompletion();

        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback).awaitCompletion();
        dockerClient.execStartCmd(execCreateCmdResponse3.getId()).exec(callback).awaitCompletion();

        System.out.println(stdout.toString());
    }

    public void sendResultsToServer(){

    }
}

//
//                dockerClient.startContainerCmd(container.getId()).exec();
//
//                ByteArrayOutputStream stdout = new ByteArrayOutputStream();
//                ByteArrayOutputStream stderr = new ByteArrayOutputStream();
//
//                ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
//                .withAttachStdout(true)
//                .withAttachStderr(true)
//                .withCmd("ls")
//                .exec();
//
//                ExecStartResultCallback callback = new ExecStartResultCallback() {
//@Override
//public void onNext(Frame frame) {
//        System.out.println("frame: " + frame);
//        super.onNext(frame);
//        }
//        };
//
////        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(
////                new ExecStartResultCallback(stdout, stderr)).awaitCompletion();
//
//        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback).awaitCompletion();
//
//        dockerClient.stopContainerCmd(container.getId()).exec();
//
//        System.out.println(stdout.toString());

