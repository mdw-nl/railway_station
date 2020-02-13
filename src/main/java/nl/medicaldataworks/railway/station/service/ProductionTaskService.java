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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.apache.http.HttpVersion.HTTP;

@Slf4j
@Service
@ConditionalOnProperty(
        value="central.simulation",
        havingValue = "false",
        matchIfMissing = true)
public class ProductionTaskService implements TaskService {
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

    @Override
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

    @Override
    public void pollForNewTasks() throws InterruptedException {
        while (true){
            try {
                log.info("Polling for tasks");
                TaskDto[] task = getNextTaskFromServer();
                if (task.length != 0) {
                    TrainDto trainDto = getTrain(task[0].getTrainId());
                    List<TaskDto> completedTasks = Arrays.asList(retrieveCompletedTasks(trainDto));
                    performTask(task[0], trainDto, completedTasks);
                }
            } catch (URISyntaxException e) {
                log.error("Error while connecting to central ", e);
            } catch (IOException e) {
                log.error("Error while accessing files on host",e);
            } catch (Exception e) { //TODO need to add specific connection to server lost error for logging
                log.error("Could not process tasks", e);
            }
            Thread.sleep(DEFAULT_SLEEP_TIME);
        }
    }

    private TaskDto[] retrieveCompletedTasks(TrainDto trainDto) throws URISyntaxException {
        URIBuilder builder = createUriBuilder();
        builder.setPath(String.format(API_TRAIN_TASKS, trainDto.getId()));
        builder.addParameter("station-name", stationName);
        builder.addParameter("calculation-status", CalculationStatus.COMPLETED.name());
        builder.addParameter("iteration", trainDto.getCurrentIteration().toString());
        builder.addParameter("access_token", getAccessToken());
        log.trace("url: {}", builder.build().toString());
        return restTemplate.getForObject(builder.build().toString(), TaskDto[].class);
    }

    @Override
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

    @Override
    public void performTask(TaskDto taskDto, TrainDto trainDto, List<TaskDto> completedTaskDtos) throws IOException, URISyntaxException {
        log.trace("Running task: {} for train: {}.", taskDto.getId(), trainDto.getId());
        taskDto.setCalculationStatus(CalculationStatus.PROCESSING);
        updateTask(taskDto);
        String id;
        try {
            id = trainRunnerService.startContainer(trainDto.getDockerImageUrl());
            try {
                String errorString = trainRunnerService.parseLogsFromTrain(id);
                log.info("Error log: {}", errorString);
                trainRunnerService.addInputToTrain(id, taskDto.getInput());//TODO filter input
                trainRunnerService.addCompletedTasksToTrain(id, completedTaskDtos);
                trainRunnerService.executeCommand(id, taskDto.isMaster());
                List<TaskDto> newTaskDtos = trainRunnerService.parseNewTasksFromTrain(id);
                taskDto.setResult(trainRunnerService.readOutputFromTrain(id));
                createNewTasks(newTaskDtos, trainDto.getId());
                taskDto.setCalculationStatus(CalculationStatus.COMPLETED);
                updateTask(taskDto);
                if(taskDto.isMaster() && newTaskDtos.isEmpty()){
                    updateTrainStatus(trainDto, CalculationStatus.COMPLETED);
                }
            }
            catch (Exception e) {
                taskDto.setCalculationStatus(CalculationStatus.ERRORED); //TODO add stack to result?
                updateTask(taskDto);
                log.error("Could not execute container.", e);
            } finally {
                trainRunnerService.stopContainer(id);
            }
        } catch (Exception e) {
            taskDto.setCalculationStatus(CalculationStatus.ERRORED);
            updateTask(taskDto);
            log.error("Could not start container.", e);
        }

    }

    private void updateTrainStatus(TrainDto trainDto, CalculationStatus calculationStatus) throws URISyntaxException {
        trainDto.setCalculationStatus(calculationStatus);
        URIBuilder builder = createUriBuilder();
        builder.setPath(String.format("/api/trains", trainDto));
        builder.addParameter("access_token", getAccessToken());
        log.trace("url: {}", builder.build().toString());
        restTemplate.put(builder.build().toString(), trainDto);
    }

    @Override
    public void updateTask(TaskDto taskDto) throws URISyntaxException {
        URIBuilder builder = createUriBuilder();
        builder.setPath(String.format("/api/trains/%s/tasks", taskDto.getTrainId()));
        builder.addParameter("access_token", getAccessToken());
        log.trace("url: {}", builder.build().toString());
        restTemplate.put(builder.build().toString(), taskDto);
    }

    @Override
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