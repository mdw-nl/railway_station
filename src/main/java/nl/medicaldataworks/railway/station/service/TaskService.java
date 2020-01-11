package nl.medicaldataworks.railway.station.service;

import nl.medicaldataworks.railway.station.web.dto.TaskDto;
import nl.medicaldataworks.railway.station.web.dto.TrainDto;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public interface TaskService {
    void startService() throws InterruptedException ;
    void pollForNewTasks() throws InterruptedException;
    TaskDto[] getNextTaskFromServer() throws URISyntaxException;
    void performTask(TaskDto taskDto, TrainDto trainDto, List<TaskDto> completedTasks) throws InterruptedException, IOException, URISyntaxException;
    void updateTask(TaskDto taskDto) throws URISyntaxException;
    void createNewTasks(List<TaskDto> taskDtos, Long trainId) throws URISyntaxException ;
}
