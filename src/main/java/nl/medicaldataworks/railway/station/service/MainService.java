package nl.medicaldataworks.railway.station.service;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class MainService {
    private ProductionTaskService taskService;

    public MainService(ProductionTaskService taskService) {
        this.taskService = taskService;
    }

    @PostConstruct
    public void startProcessing() throws InterruptedException {
        taskService.startService();
    }
}
