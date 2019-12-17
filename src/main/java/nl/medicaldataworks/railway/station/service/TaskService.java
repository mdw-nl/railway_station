package nl.medicaldataworks.railway.station.service;

import lombok.extern.slf4j.Slf4j;
import nl.medicaldataworks.railway.station.config.CentralConfiguration;
import nl.medicaldataworks.railway.station.web.dto.TaskDto;
import nl.medicaldataworks.railway.station.web.dto.TrainDto;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.net.URISyntaxException;

import static org.apache.http.HttpVersion.HTTP;

@Service
@Slf4j
public class TaskService {
    public static final String TASK_API_PATH =  "/api/tasks";
    private WebClient webClient;
    private CentralConfiguration centralConfig;
    private TrainRunnerService trainRunnerService;

    public TaskService(WebClient webClient, CentralConfiguration centralConfig, TrainRunnerService trainRunnerService) {
        this.webClient = webClient;
        this.centralConfig = centralConfig;
        this.trainRunnerService =trainRunnerService;
        this.pollForNewTasks();
    }

    public void pollForNewTasks(){
        while (true){
            try {
                log.info("Is there a new task?");
                TaskDto[] task = getNextTaskFromServer();
                if (task.length == 0) {
                    Thread.sleep(1000);
                } else {
                    TrainDto trainDto = getTrain(task[0].getTrain());
                    performTask(task[0], trainDto);
                }

            } catch (URISyntaxException e) {
                log.error("Unable to query central API for train.", e);
            } catch (InterruptedException e) {
                log.error("Wait timer interrupted: {}",e);
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
        builder.setParameter("station-id", "maastro");
        builder.setParameter("page", "0");
        builder.setParameter("sort", "creationTimestamp");
        builder.setParameter("creationTimestamp.dir", "desc");
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

    public void performTask(TaskDto taskDto, TrainDto trainDto) {
        log.info("Running task: {} for train: {}.", taskDto.getId(), trainDto.getId());
        //TODO add master or client switch
        String id;
        try {
            id = trainRunnerService.startContainer(trainDto.getDockerImageUrl());
        } catch (Exception e) {
            log.error("Could not start container: {}", e);
            return;
        }
        try {
            trainRunnerService.executeCommand(id);
            trainRunnerService.readOutputFile(id);
        }
        catch (Exception e) {
            log.error("Could not execute container: {}", e);
        }
        finally {
            try {
                trainRunnerService.stopContainer(id);
            } catch (Exception e) {
                log.error("Could not stop container: {}", e);
            } finally {
                pollForNewTasks();
            }
        }

    }

    public void sendResultsToServer(){
        //TODO update task with result
    }
}