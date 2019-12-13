package nl.medicaldataworks.railway.station.service;

import lombok.extern.slf4j.Slf4j;
import nl.medicaldataworks.railway.station.config.CentralConfiguration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class TrainRunnerService {
    private WebClient webClient;
    private CentralConfiguration centralConfig;
    private static final String API_PATH = "/api/";

    public TrainRunnerService(WebClient webClient, CentralConfiguration centralConfig) {
        this.webClient = webClient;
        this.centralConfig = centralConfig;
    }

    @Scheduled(fixedDelay = 1000)
    public void getNextTaskFromServer(){
        Mono<String> body = this.webClient
                .get()
                .uri("/api/tasks")
                .retrieve()
                .bodyToMono(String.class);
        body.subscribe(s -> wtf(s));
    }

    private void wtf2(Throwable s) {
        log.error("error: {}", s);
    }

    private void wtf(String s) {
        log.info("response: {}", s);
        System.out.println("whattt" + s);
        log.info("response: {}", s);
    }

    public void runTrain(){

    }

    public void sendResultsToServer(){

    }
}
//
//    DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
//            .withDockerHost("tcp://localhost:2375")
//            .withDockerTlsVerify(false)
//            .withDockerCertPath("C:\\Users\\user\\.docker\\certs")
//            .withDockerConfig("C:\\Users\\user\\.docker")
//            .withApiVersion("1.30") // optional
//            .withRegistryUrl("https://registry.hub.docker.com/")
//            .withRegistryUsername("user")
//            .withRegistryPassword("password")
//            .withRegistryEmail("email")
//            .build();
//    DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();
//
//        dockerClient.pullImageCmd("postgres:latest").exec(new PullImageResultCallback()).awaitCompletion();
//
//                CreateContainerResponse container = dockerClient.createContainerCmd("postgres:latest")
//                .withAttachStderr(true)
//                .withAttachStdout(true)
//                .exec();
//                String containerId = container.getId();
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

