package com.testeBanda.testador.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SchedulerService {
    @Autowired
    private TesterService testerService;

    @Autowired
    private QuedaService quedaService;

    @Scheduled(cron = "0 0 0,23 * * ?")
    public void schedulerTest() {
        System.out.println("Scheduler Test");
        testerService.iniciarTeste("");
    }

    @Scheduled(cron = "30 00 10 * * ?")
    public void schedulerTestFalhas(){
        System.out.println("Scheduler TestFalhas");
        testerService.ligarTesteFalhas();
        testerService.iniciarTeste("");
    }

    @Scheduled(fixedDelay = 60000)
    public void schedulerQuedas(){
        System.out.println("*** ATUALIZANDO QUEDAS ***");
        quedaService.atualizaQuedas();
    }


}
