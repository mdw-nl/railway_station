package nl.medicaldataworks.railway.station.service;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class MainService {
    private TaskService taskService;

    public MainService(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostConstruct
    public void startProcessing() throws InterruptedException {
        taskService.startService();
    }
}
