package nl.medicaldataworks.railway.station.service;


import lombok.extern.slf4j.Slf4j;
import nl.medicaldataworks.railway.station.config.CentralConfiguration;
import nl.medicaldataworks.railway.station.domain.CalculationStatus;
import nl.medicaldataworks.railway.station.domain.TokenObject;
import nl.medicaldataworks.railway.station.web.dto.StationDto;
import nl.medicaldataworks.railway.station.web.dto.TaskDto;
import nl.medicaldataworks.railway.station.web.dto.TrainDto;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.apache.http.HttpVersion.HTTP;

@Slf4j
@Service
@ConditionalOnProperty(
        value="central.simulation",
        havingValue = "false",
        matchIfMissing = true)
public class ProductionTaskService {
    public static final String TASK_API_PATH =  "/api/tasks";
    public static final String API_STATIONS =  "/api/stations";
    public static final String STATION_NAME_PARAM =  "station-name";
    public static final String API_TRAINS = "/api/trains/%s";
    public static final String API_TRAIN_TASKS = API_TRAINS + "/tasks";
    public static final String HTTPS = "https";

    public final RestTemplate restTemplate;

    private CentralConfiguration centralConfig;
    private TrainRunnerService trainRunnerService;
    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String stationName;
    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String stationSecret;
    @Value("${spring.security.oauth2.client.provider.keycloak.token-uri}")
    private String tokenUrl = "https://dcra-keycloak.railway.medicaldataworks.nl/auth/realms/railway/protocol/openid-connect/token";
    private static final long DEFAULT_SLEEP_TIME = 5000;

    public ProductionTaskService(RestTemplate restTemplate, CentralConfiguration centralConfig,
                                 TrainRunnerService trainRunnerService) {
        this.restTemplate = restTemplate;
        this.centralConfig = centralConfig;
        this.trainRunnerService = trainRunnerService;
    }

    public void startService() throws InterruptedException {
        if(!isClientConfigurationValid()){
            log.info("Shutting down because the station name could not be validated.");
            System.exit(0);
        }
        pollForNewTasks();
    }

    HttpEntity<Object> createHttpEntity(){
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        HttpHeaders httpHeaders =  new HttpHeaders() {{
            String auth = stationName + ":" + stationSecret;
            byte[] encodedAuth = Base64.encodeBase64(
                    auth.getBytes(StandardCharsets.US_ASCII) );
            String authHeader = "Basic " + new String( encodedAuth );
            set( "Authorization", authHeader );
        }};
        return new HttpEntity<>(body, httpHeaders);
    }

    String getAccessToken(){
        HttpEntity<Object> httpEntity = createHttpEntity();
        TokenObject tokenObject = restTemplate.postForObject(tokenUrl, httpEntity, TokenObject.class);
        return tokenObject.getAccess_token();
    }

    public boolean isClientConfigurationValid(){
        URIBuilder builder = createUriBuilder();
        builder.setPath(API_STATIONS);
        builder.addParameter(STATION_NAME_PARAM, stationName);


        try {
            builder.addParameter("access_token", getAccessToken());
            StationDto[] stations = restTemplate.getForObject(builder.build().toString(), StationDto[].class);
            if(stations.length == 0){
                return false;
            }
        } catch (URISyntaxException e) {
            log.error("invalid URI syntact in station name validation", e);
        }
        return true;
    }

    private URIBuilder createUriBuilder() {
        URIBuilder builder = new URIBuilder();
        if((centralConfig.getPort() % 1000) == 443){
            builder.setScheme(HTTPS);
        } else {
            builder.setScheme(HTTP);
        }
        builder.setHost(centralConfig.getHostname());
        builder.setPort(centralConfig.getPort());
        return builder;
    }

    public void pollForNewTasks() throws InterruptedException {
        while (true){
            try {
                log.info("Polling for tasks");
                TaskDto[] task = getNextTaskFromServer();
                if (task.length != 0) {
                    performTask(task[0]);
                }
            } catch (URISyntaxException e) {
                log.error("Error while connecting to central ", e);
            } catch (Exception e) {
                log.error("Could not process tasks", e);
            }
            Thread.sleep(DEFAULT_SLEEP_TIME);
        }
    }

    private TaskDto[] retrieveCompletedTasks(TrainDto trainDto) throws URISyntaxException {
        URIBuilder builder = createUriBuilder();
        builder.setPath(String.format(API_TRAIN_TASKS, trainDto.getId()));
        builder.addParameter("calculation-status", CalculationStatus.COMPLETED.name());
        builder.addParameter("iteration", trainDto.getCurrentIteration().toString());
        builder.addParameter("access_token", getAccessToken());
        log.trace("url: {}", builder.build().toString());
        return restTemplate.getForObject(builder.build().toString(), TaskDto[].class);
    }

    public TaskDto[] getNextTaskFromServer() throws URISyntaxException {
        URIBuilder builder = createUriBuilder();
        builder.setPath(TASK_API_PATH);
        builder.addParameter("page", "0");
        builder.addParameter("size", "1");
        builder.addParameter("sort", "creationTimestamp");
        builder.addParameter("station-name", stationName);

        builder.addParameter("calculation-status", CalculationStatus.REQUESTED.name());
        builder.addParameter("access_token", getAccessToken());
        log.trace("url: {}", builder.build().toString());
        return restTemplate.getForObject(builder.build().toString(), TaskDto[].class);
    }

    private TrainDto getTrain(Long id) throws URISyntaxException {
        URIBuilder builder = createUriBuilder();
        builder.setPath(String.format(API_TRAINS, id));
        builder.addParameter("access_token", getAccessToken());
        log.trace("url: {}", builder.build().toString());
        return restTemplate.getForObject(builder.build().toString(), TrainDto.class);
    }

    public void performTask(TaskDto taskDto) throws URISyntaxException {
        log.info("Running task: {}.", taskDto.getId());
        TrainDto trainDto = getTrain(taskDto.getTrainId());
        if(trainDto.getCalculationStatus().equals(CalculationStatus.REQUESTED)){
            trainDto.setCalculationStatus(CalculationStatus.PROCESSING);
            updateTrain(trainDto);
        }
        taskDto.setCalculationStatus(CalculationStatus.PROCESSING);
        updateTask(taskDto);
        String containerId;
        try {
            containerId = trainRunnerService.startContainer(trainDto.getDockerImageUrl());
            try {
                trainRunnerService.addInputToTrain(containerId, taskDto.getInput());
                if(taskDto.isMaster()){
                    List<TaskDto> completedTasks = Arrays.asList(retrieveCompletedTasks(trainDto));
                    trainRunnerService.addCompletedTasksToTrain(containerId, completedTasks);
                }
                trainRunnerService.executeCommand(containerId, taskDto.isMaster());
                processTrainResults(trainDto, taskDto, containerId);
            }
            catch (Exception e) {
                handleTrainException(containerId, trainDto, taskDto, e);
            } finally {
                trainRunnerService.stopContainer(containerId);
            }
        } catch (Exception e) {
            handleTrainException(UUID.randomUUID().toString(), trainDto, taskDto, e);
        }
    }

    private void processTrainResults(TrainDto trainDto, TaskDto taskDto, String containerId) throws Exception {
        taskDto.setLogLocation(trainRunnerService.parseAppLogsFromTrain(containerId));
        trainRunnerService.parseErrorLogsFromTrain(containerId);
        List<TaskDto> newTaskDtos = trainRunnerService.parseNewTasksFromTrain(containerId);
        createNewTasks(newTaskDtos, trainDto.getId());
        //Train is updated first to prevent timing errors when creating new master tasks in Central
        updateTrainStatus(trainDto, taskDto, newTaskDtos);
        taskDto.setResult(trainRunnerService.readOutputFromTrain(containerId));
        taskDto.setCalculationStatus(CalculationStatus.COMPLETED);
        updateTask(taskDto);
    }

    private void updateTrainStatus(TrainDto trainDto, TaskDto taskDto, List<TaskDto> newTaskDtos) throws URISyntaxException {
        if(taskDto.isMaster()) {
            if(newTaskDtos.isEmpty()){
                trainDto.setCalculationStatus(CalculationStatus.COMPLETED);
                updateTrain(trainDto);
            } else {
                trainDto.setClientTaskCount(Integer.toUnsignedLong(newTaskDtos.size()));
                trainDto.setCurrentIteration(trainDto.getCurrentIteration() + 1);
                updateTrain(trainDto);
            }
        }
    }

    private void handleTrainException(String containerId, TrainDto trainDto, TaskDto taskDto, Exception e) throws URISyntaxException {
        taskDto.setCalculationStatus(CalculationStatus.ERRORED);
        taskDto.setError("UUID: ".concat(containerId).concat("\n message: ").concat(e.getMessage()));
        updateTask(taskDto);
        trainDto.setCalculationStatus(CalculationStatus.ERRORED);
        updateTrain(trainDto);
        log.error("Could not execute container. UUID: {}", containerId, e);
    }

    private void updateTrain(TrainDto trainDto) throws URISyntaxException {
        URIBuilder builder = createUriBuilder();
        builder.setPath(String.format("/api/trains", trainDto));
        builder.addParameter("access_token", getAccessToken());
        log.trace("url: {}", builder.build().toString());
        restTemplate.put(builder.build().toString(), trainDto);
    }

    public void updateTask(TaskDto taskDto) throws URISyntaxException {
        URIBuilder builder = createUriBuilder();
        builder.setPath(String.format("/api/trains/%s/tasks", taskDto.getTrainId()));
        builder.addParameter("access_token", getAccessToken());
        log.trace("url: {}", builder.build().toString());
        restTemplate.put(builder.build().toString(), taskDto);
    }

    public void createNewTasks(List<TaskDto> taskDtos, Long trainId) throws URISyntaxException {
        for (TaskDto taskDto: taskDtos ) {
            URIBuilder builder = createUriBuilder();
            builder.setPath(String.format("/api/trains/%s/tasks", trainId));
            builder.addParameter("access_token", getAccessToken());
            log.trace("url: {}", builder.build().toString());
            restTemplate.postForLocation(builder.build().toString(), taskDto);
        }
    }
}