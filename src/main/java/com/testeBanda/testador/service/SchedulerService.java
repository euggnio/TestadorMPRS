package com.testeBanda.testador.service;

import ch.qos.logback.core.util.FixedDelay;
import com.testeBanda.testador.api.SnmpWanMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SchedulerService {
    @Autowired
    private TesterService testerService;

    @Autowired
    private QuedaService quedaService;

    @Autowired
    private SnmpWanMonitor monitor;

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

    @Scheduled(cron = "0 0 0,22 * * ?")
    public void revisaTodasQuedas() {
        log.info("Revisando todas as quedas do banco com o Nagios");
        quedaService.revisaTodasQuedas();
    }

    @Scheduled(fixedDelay = 60000)
    public void schedulerQuedas(){
        log.debug("ATUALIZANDO QUEDAS");
        quedaService.atualizaQuedas();
    }

    @Scheduled(fixedDelay = 5000)
    private void rodarSnmp(){
        log.debug(" == ATUALIZANDO SNMP == ");
        monitor.run();
    }



}
