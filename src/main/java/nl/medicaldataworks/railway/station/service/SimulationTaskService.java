package nl.medicaldataworks.railway.station.service;

import lombok.extern.slf4j.Slf4j;
import nl.medicaldataworks.railway.station.web.dto.TaskDto;
import nl.medicaldataworks.railway.station.web.dto.TrainDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;

@Slf4j
@Service
@ConditionalOnProperty(
        value="central.simulation",
        havingValue = "true")
public class SimulationTaskService implements TaskService {


    public void startService(){

    }
    public void pollForNewTasks() {
        log.info("started simulation succesfully");
    }
    public TaskDto[] getNextTaskFromServer() throws URISyntaxException {
        TaskDto[] taskDtos = new TaskDto[1];
        return taskDtos;
    }

    public void performTask(TaskDto taskDto, TrainDto trainDto) throws InterruptedException, IOException, URISyntaxException {

    }

    public void updateTaskDto(TaskDto taskDto) throws URISyntaxException {

    }
}
