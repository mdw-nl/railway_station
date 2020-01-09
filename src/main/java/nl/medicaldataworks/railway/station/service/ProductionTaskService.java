package nl.medicaldataworks.railway.station.service;


import lombok.extern.slf4j.Slf4j;
import nl.medicaldataworks.railway.station.config.CentralConfiguration;
import nl.medicaldataworks.railway.station.domain.CalculationStatus;
import nl.medicaldataworks.railway.station.web.dto.StationDto;
import nl.medicaldataworks.railway.station.web.dto.TaskDto;
import nl.medicaldataworks.railway.station.web.dto.TrainDto;
import org.apache.http.client.utils.URIBuilder;
import org.bouncycastle.jce.provider.AnnotatedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.Exceptions.*;
import java.io.IOException;
import java.net.URISyntaxException;

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

    public void pollForNewTasks() throws InterruptedException {
        while (true){
            try {
                log.info("Polling for tasks");
                TaskDto[] task = getNextTaskFromServer();
                if (task.length != 0) {
                    TrainDto trainDto = getTrain(task[0].getTrainId());
                    performTask(task[0], trainDto);
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
                log.error("Could not process tasks");
            }
            Thread.sleep(DEFAULT_SLEEP_TIME);
        }
    }

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

        builder.addParameter("calculation-status", String.valueOf(CalculationStatus.REQUESTED));
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
        builder.setPath(String.format("/api/trains/%s", id));
        return webClient
                .get()
                .uri(builder.build().toString())
                .retrieve()
                .bodyToMono(TrainDto.class)
                .block();
    }

    public void performTask(TaskDto taskDto, TrainDto trainDto) throws InterruptedException, IOException, URISyntaxException {
        log.info("Running task: {} for train: {}.", taskDto.getId(), trainDto.getId());
        taskDto.setCalculationStatus(CalculationStatus.PROCESSING);
        updateTaskDto(taskDto);
        String id = trainRunnerService.startContainer(trainDto.getDockerImageUrl());

        try {
            trainRunnerService.addInputToTrain(id, taskDto.getInput()); //TODO filter input
            trainRunnerService.executeCommand(id, taskDto.isMaster());
            taskDto.setResult(trainRunnerService.readOutputFromTrain(id));
            taskDto.setCalculationStatus(CalculationStatus.COMPLETED);
            updateTaskDto(taskDto);
        }
        catch (Exception e) {
            log.error("Could not execute container.", e);
        }
        finally {
            trainRunnerService.stopContainer(id);
        }
    }

    public void updateTaskDto(TaskDto taskDto) throws URISyntaxException {
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
}