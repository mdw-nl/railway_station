package nl.medicaldataworks.railway.station.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.medicaldataworks.railway.station.config.CentralConfiguration;
import nl.medicaldataworks.railway.station.domain.CalculationStatus;
import nl.medicaldataworks.railway.station.web.dto.TaskDto;
import nl.medicaldataworks.railway.station.web.dto.TrainDto;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.apache.http.HttpVersion.HTTP;

@Service
@Slf4j
public class TaskService {
    public static final String TASK_API_PATH =  "/api/tasks";
    private WebClient webClient;
    private CentralConfiguration centralConfig;
    private TrainRunnerService trainRunnerService;

    public TaskService(WebClient webClient,
                       CentralConfiguration centralConfig,
                       TrainRunnerService trainRunnerService) {
        this.webClient = webClient;
        this.centralConfig = centralConfig;
        this.trainRunnerService =trainRunnerService;
        this.pollForNewTasks();
    }

    public void pollForNewTasks(){
        while (true){
            try {
                log.trace("Polling for tasks");
                TaskDto[] task = getNextTaskFromServer();
                if (task.length == 0) {
                    Thread.sleep(1000);
                } else {
                    TrainDto trainDto = getTrain(task[0].getTrain());
                    performTask(task[0], trainDto);
                }

            } catch (URISyntaxException e) {
                log.error("Error while connecting to central: {}", e);
            } catch (InterruptedException e) {
                log.error("Wait timer interrupted: {}",e);
            } catch (IOException e) {
                log.error("Error while accessing files on host: {}",e);
            } catch (Exception e) {
                log.error("Could not process tasks: {}",e);
            } //TODO added specific error when central is not accessible
        }
    }

    public TaskDto[] getNextTaskFromServer() throws URISyntaxException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(HTTP);
        builder.setHost(centralConfig.getHostname());
        builder.setPort(centralConfig.getPort());
        builder.setPath(TASK_API_PATH);
        builder.addParameter("station", "1");
        builder.addParameter("page", "0");
        builder.addParameter("size", "1");
        builder.addParameter("sort", "creationTimestamp");
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
        //TODO add master or client switch
        String id = trainRunnerService.startContainer(trainDto.getDockerImageUrl());

        try {
            trainRunnerService.addInputToTrain(id, taskDto.toString()); //TODO filter input
            trainRunnerService.executeCommand(id);
            taskDto.setResult(trainRunnerService.readOutputFromTrain(id));
            taskDto.setCalculationStatus(CalculationStatus.COMPLETED);
            updateTaskDto(taskDto);
        }
        catch (Exception e) {
            log.error("Could not execute container: {}", e);
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
        builder.setPath(String.format("/api/trains/%s/tasks", taskDto.getTrain()));
        webClient
                .put()
                .uri(builder.build().toString())
                .body(BodyInserters.fromValue(taskDto))
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}