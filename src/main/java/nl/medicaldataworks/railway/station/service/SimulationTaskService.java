package nl.medicaldataworks.railway.station.service;

import lombok.extern.slf4j.Slf4j;
import nl.medicaldataworks.railway.station.web.dto.TaskDto;
import nl.medicaldataworks.railway.station.web.dto.TrainDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(
        value="central.simulation",
        havingValue = "true")
public class SimulationTaskService implements TaskService {

    @Override
    public void startService(){

    }

    @Override
    public void pollForNewTasks() {
        log.info("started simulation succesfully");
    }

    @Override
    public TaskDto[] getNextTaskFromServer() throws URISyntaxException {
        TaskDto[] taskDtos = new TaskDto[1];
        return taskDtos;
    }

    @Override
    public void performTask(TaskDto taskDto, TrainDto trainDto) throws InterruptedException, IOException, URISyntaxException {

    }

    @Override
    public void updateTask(TaskDto taskDto) throws URISyntaxException {

    }

    @Override
    public void createNewTasks(List<TaskDto> taskDtos, Long trainId) throws URISyntaxException {

    }
}
