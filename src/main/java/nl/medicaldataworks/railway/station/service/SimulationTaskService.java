package nl.medicaldataworks.railway.station.service;

import lombok.extern.slf4j.Slf4j;
import nl.medicaldataworks.railway.station.config.CentralConfiguration;
import nl.medicaldataworks.railway.station.domain.CalculationStatus;
import nl.medicaldataworks.railway.station.web.dto.TaskDto;
import nl.medicaldataworks.railway.station.web.dto.TrainDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(
        value="central.simulation",
        havingValue = "true")
public class SimulationTaskService implements TaskService {

    private static final String DOCKER_URL = "registry.gitlab.com/medicaldataworks/railway/prototypetrain:master";
    private final CentralConfiguration centralConfig;
    private final TrainRunnerService trainRunnerService;
    private List<TaskDto> SimulationTasksCompleted = new ArrayList<TaskDto>();
    private List<TaskDto> SimulationTasksIdle = new ArrayList<TaskDto>();
    private List<TaskDto> SimulationTasksArchived = new ArrayList<TaskDto>();
    private List<TaskDto> SimulationTaskQueue = new ArrayList<TaskDto>();
    private static final long DEFAULT_SLEEP_TIME = 5000;

    public SimulationTaskService(CentralConfiguration centralConfig,
                                 TrainRunnerService trainRunnerService) {
        this.centralConfig = centralConfig;
        this.trainRunnerService = trainRunnerService;
    }

    @Override
    public void startService() throws InterruptedException {
        log.info("started simulation succesfully");
        masterRun1();
        stationRuns();
        masterRun2();
        System.exit(0);
    }

    @Override
    public void pollForNewTasks() throws InterruptedException {
        String id = trainRunnerService.startContainer(DOCKER_URL);
        try {
            TaskDto taskDto = SimulationTaskQueue.get(0);
            trainRunnerService.addInputToTrain(id, taskDto.getInput());//TODO filter input
            trainRunnerService.addCompletedTasksToTrain(id, SimulationTasksCompleted);
            trainRunnerService.executeCommand(id, taskDto.isMaster());
            List<TaskDto> newTaskDtos = trainRunnerService.parseNewTasksFromTrain(id);
            taskDto.setResult(trainRunnerService.readOutputFromTrain(id));
            determineIdleOrCompletedCalculationStatus(taskDto, newTaskDtos);
            if (taskDto.getCalculationStatus().equals(CalculationStatus.IDLE)){
                SimulationTasksIdle.add(taskDto);
            } else {
                SimulationTasksCompleted.add(taskDto);
            }
            SimulationTaskQueue.remove(0);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    @Override
    public TaskDto[] getNextTaskFromServer() throws URISyntaxException {
        TaskDto[] taskDtos = new TaskDto[1];
        return taskDtos;
    }

    @Override
    public void performTask(TaskDto taskDto, TrainDto trainDto, List<TaskDto> completedTasks) throws InterruptedException, IOException, URISyntaxException {

    }

    @Override
    public void updateTask(TaskDto taskDto) throws URISyntaxException {

    }

    @Override
    public void createNewTasks(List<TaskDto> taskDtos, Long trainId) throws URISyntaxException {

    }

    private void determineIdleOrCompletedCalculationStatus(TaskDto taskDto, List<TaskDto> taskDtos) {
        if (!taskDtos.isEmpty() && taskDto.isMaster()) {
            taskDto.setCalculationStatus(CalculationStatus.IDLE);
        } else {
            taskDto.setCalculationStatus(CalculationStatus.COMPLETED);
        }
    }

    private void masterRun1() throws InterruptedException {
        SimulationTasksCompleted.addAll(generateCompletedTasksMasterRun1());
        SimulationTaskQueue.add(generateMasterTask1());
        while (!SimulationTaskQueue.isEmpty()){
            pollForNewTasks();
        }
    }

    private void stationRuns(){

    }

    private void masterRun2(){

    }

    private TrainDto generateTrain(){
        TrainDto train = new TrainDto();
        return train;
    }

    private List<TaskDto> generateCompletedTasksMasterRun1(){
        List<TaskDto> tasks =  new ArrayList<TaskDto>();
        TaskDto firstStationInput = new TaskDto();
        firstStationInput.setId(1L);
        firstStationInput.setCalculationStatus(CalculationStatus.COMPLETED);
        firstStationInput.setResult("");
        firstStationInput.setStationId(1L);
        firstStationInput.setTrainId(1L);
        firstStationInput.setInput("5");
        firstStationInput.setMaster(false);
        tasks.add(firstStationInput);

        TaskDto secondStationInput = new TaskDto();
        secondStationInput.setId(1L);
        secondStationInput.setCalculationStatus(CalculationStatus.COMPLETED);
        secondStationInput.setResult("");
        secondStationInput.setStationId(1L);
        secondStationInput.setTrainId(1L);
        secondStationInput.setInput("5");
        secondStationInput.setMaster(false);
        tasks.add(secondStationInput);
        return tasks;
    }

    private TaskDto generateMasterTask1(){
        TaskDto firstMasterInput = new TaskDto();
        firstMasterInput.setId(1L);
        firstMasterInput.setCalculationStatus(CalculationStatus.REQUESTED);
        firstMasterInput.setResult("");
        firstMasterInput.setStationId(1L);
        firstMasterInput.setTrainId(1L);
        firstMasterInput.setInput("");
        firstMasterInput.setMaster(true);
        return firstMasterInput;
    }

    private List<TaskDto> generateCompletedTasksMasterRun2(){
        return new ArrayList<TaskDto>();
    }

    private List<TaskDto> generateMasterTask2(){
        return new ArrayList<TaskDto>();
    }


}
