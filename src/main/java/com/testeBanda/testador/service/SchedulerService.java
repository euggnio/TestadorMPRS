package com.testeBanda.testador.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SchedulerService {
    @Autowired
    private TesterService testerService;

    @Autowired
    private QuedaService quedaService;

    @Scheduled(cron = "0 0 0,23 * * ?")
    public void schedulerTest() {
        log.info("Iniciando teste diário de banda");
        testerService.iniciarTeste("");
    }

    @Scheduled(cron = "30 00 10 * * ?")
    public void schedulerTestFalhas(){
        log.info("Iniciando segundo teste de banda para unidades que falharam o teste diário");
        testerService.ligarTesteFalhas();
        testerService.iniciarTeste("");
    }

    @Scheduled(fixedDelay = 60000)
    public void schedulerQuedas(){
        log.debug("ATUALIZANDO QUEDAS");
        quedaService.atualizaQuedas();
    }


}
