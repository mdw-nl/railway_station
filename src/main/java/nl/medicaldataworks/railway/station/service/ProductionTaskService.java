package nl.medicaldataworks.railway.station.service;


import lombok.extern.slf4j.Slf4j;
import nl.medicaldataworks.railway.station.config.CentralConfiguration;
import nl.medicaldataworks.railway.station.domain.CalculationStatus;
import nl.medicaldataworks.railway.station.web.dto.StationDto;
import nl.medicaldataworks.railway.station.web.dto.TaskDto;
import nl.medicaldataworks.railway.station.web.dto.TrainDto;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.URISyntaxException;
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
    public static final String VALIDATION_API_PATH =  "/api/stations/validate/";
    public static final String API_TRAINS = "/api/trains/%s";
    public static final String API_TRAIN_TASKS = API_TRAINS + "/tasks";

    private WebClient webClient;
    private CentralConfiguration centralConfig;
    private TrainRunnerService trainRunnerService;
    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String stationName;
    private static final long DEFAULT_SLEEP_TIME = 5000;

    public ProductionTaskService(WebClient webClient,
                                 CentralConfiguration centralConfig,
                                 TrainRunnerService trainRunnerService) {
        this.webClient = webClient;
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

    public boolean isClientConfigurationValid(){
        URIBuilder builder = new URIBuilder();
        builder.setScheme(HTTP);
        builder.setHost(centralConfig.getHostname());
        builder.setPort(centralConfig.getPort());
        builder.setPath(VALIDATION_API_PATH.concat(stationName));
        try {
            webClient
                    .get()
                    .uri(builder.build().toString())
                    .retrieve()
                    .bodyToMono(StationDto.class)
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()){
                log.error("invalid station name: {}", stationName, e);
            } else {
                log.error("error while validation station name {}.", stationName, e);
            }
            return false;
        } catch (URISyntaxException e) {
            log.error("invalid URI syntact in station name validation", e);
        }
        return true;
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
            } catch (WebClientResponseException e) {
                log.info("",e);
            } catch (URISyntaxException e) {
                log.error("Error while connecting to central ", e);
            } catch (InterruptedException e) {
                log.error("Wait timer interrupted",e);
            } catch (IOException e) {
                log.error("Error while accessing files on host",e);
            } catch (Exception e) { //TODO need to add specific connection to server lost error for logging
                log.error("Could not process tasks", e);
            }
            Thread.sleep(DEFAULT_SLEEP_TIME);
        }
    }

    private TaskDto[] retrieveCompletedTasks(TrainDto trainDto) throws URISyntaxException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(HTTP);
        builder.setHost(centralConfig.getHostname());
        builder.setPort(centralConfig.getPort());
        builder.setPath(String.format(API_TRAIN_TASKS, trainDto.getId()));
        builder.addParameter("page", "0");
        builder.addParameter("size", "1");
        builder.addParameter("sort", "creationTimestamp");
        builder.addParameter("station-name", stationName);

        builder.addParameter("calculation-status", CalculationStatus.COMPLETED.name());
        return webClient
                .get()
                .uri(builder.build().toString())
                .retrieve()
                .bodyToMono(TaskDto[].class)
                .block();
    }

    @Override
    public TaskDto[] getNextTaskFromServer() throws URISyntaxException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(HTTP);
        builder.setHost(centralConfig.getHostname());
        builder.setPort(centralConfig.getPort());
        builder.setPath(TASK_API_PATH);
        builder.addParameter("page", "0");
        builder.addParameter("size", "1");
        builder.addParameter("sort", "creationTimestamp");
        builder.addParameter("station-name", stationName);

        builder.addParameter("calculation-status", CalculationStatus.REQUESTED.name());
        return webClient
                .get()
                .uri(builder.build().toString())
                .retrieve()
                .bodyToMono(TaskDto[].class)
                .block();
    }

    private TrainDto getTrain(Long id) throws URISyntaxException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(HTTP);
        builder.setHost(centralConfig.getHostname());
        builder.setPort(centralConfig.getPort());
        builder.setPath(String.format(API_TRAINS, id));
        return webClient
                .get()
                .uri(builder.build().toString())
                .retrieve()
                .bodyToMono(TrainDto.class)
                .block();
    }

    @Override
    public void performTask(TaskDto taskDto, TrainDto trainDto, List<TaskDto> completedTaskDtos) throws InterruptedException, IOException, URISyntaxException {
        log.info("Running task: {} for train: {}.", taskDto.getId(), trainDto.getId());
        taskDto.setCalculationStatus(CalculationStatus.PROCESSING);
        updateTask(taskDto);
        String id = trainRunnerService.startContainer(trainDto.getDockerImageUrl());
        try {
            trainRunnerService.addInputToTrain(id, taskDto.getInput());//TODO filter input
            trainRunnerService.addCompletedTasksToTrain(id, completedTaskDtos);
            trainRunnerService.executeCommand(id, taskDto.isMaster());
            List<TaskDto> newTaskDtos = trainRunnerService.parseNewTasksFromTrain(id);
            taskDto.setResult(trainRunnerService.readOutputFromTrain(id));
            createNewTasks(newTaskDtos, trainDto.getId());
            determineIdleOrCompletedCalculationStatus(taskDto, newTaskDtos);
            updateTask(taskDto);
        }
        catch (Exception e) {
            taskDto.setCalculationStatus(CalculationStatus.ERRORED); //TODO add stack to result?
            updateTask(taskDto);
            log.error("Could not execute container.", e);
        }
        finally {
            trainRunnerService.stopContainer(id);
        }
    }

    private void determineIdleOrCompletedCalculationStatus(TaskDto taskDto, List<TaskDto> taskDtos) {
        if (!taskDtos.isEmpty() && taskDto.isMaster()) {
            taskDto.setCalculationStatus(CalculationStatus.IDLE);
        } else {
            taskDto.setCalculationStatus(CalculationStatus.COMPLETED);
        }
    }

    @Override
    public void updateTask(TaskDto taskDto) throws URISyntaxException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(HTTP);
        builder.setHost(centralConfig.getHostname());
        builder.setPort(centralConfig.getPort());
        builder.setPath(String.format("/api/trains/%s/tasks", taskDto.getTrainId()));
        webClient
            .put()
            .uri(builder.build().toString())
            .body(BodyInserters.fromValue(taskDto))
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    @Override
    public void createNewTasks(List<TaskDto> taskDtos, Long trainId) throws URISyntaxException {
        for (TaskDto taskDto: taskDtos ) {
            URIBuilder builder = new URIBuilder();
            builder.setScheme(HTTP);
            builder.setHost(centralConfig.getHostname());
            builder.setPort(centralConfig.getPort());
            builder.setPath(String.format("/api/trains/%s/tasks", trainId));
            webClient
                .post()
                .uri(builder.build().toString())
                .body(BodyInserters.fromValue(taskDto))
                .retrieve()
                .toBodilessEntity()
                .block();
        }
    }
}