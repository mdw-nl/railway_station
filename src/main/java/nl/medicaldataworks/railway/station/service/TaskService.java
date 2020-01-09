package nl.medicaldataworks.railway.station.service;

import nl.medicaldataworks.railway.station.web.dto.TaskDto;
import nl.medicaldataworks.railway.station.web.dto.TrainDto;

import java.io.IOException;
import java.net.URISyntaxException;

public interface TaskService {
    void startService() throws InterruptedException ;
    void pollForNewTasks() throws InterruptedException;
    TaskDto[] getNextTaskFromServer() throws URISyntaxException;
    void performTask(TaskDto taskDto, TrainDto trainDto) throws InterruptedException, IOException, URISyntaxException;
    void updateTaskDto(TaskDto taskDto) throws URISyntaxException;
}
