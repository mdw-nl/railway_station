package nl.medicaldataworks.railway.station.service;

import lombok.extern.slf4j.Slf4j;
import nl.medicaldataworks.railway.station.config.CentralConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.function.ServerResponse;
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
        webClient.get()
                .uri("http://localhost:8080/api/tasks")
                .retrieve()
                .bodyToFlux(String.class)
                .subscribe(s -> log.info("response: {}", s));


//        URIBuilder builder = new URIBuilder();
////        builder.setScheme(HTTP);
////        builder.setHost(centralConfig.getHostname());
////        builder.setPort(centralConfig.getPort());
////        builder.setPath(API_PATH);
////        builder.setParameter("dest", "/archive/projects/" + rchiveConfig.getProjectId());
////        HttpHeaders headers = new HttpHeaders();
////        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
////        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
////        FileSystemResource fileSystemResource = new FileSystemResource(zipFilePath);
////        body.add("File", fileSystemResource);
////        HttpEntity<MultiValueMap<String, Object>> requestEntity
////                = new HttpEntity<>(body, headers);
////        ResponseEntity<String> response = restTemplate.exchange(builder.build(),
////                HttpMethod.POST,
////                requestEntity,
////                String.class);
////        log.info("response: {}", response);
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

