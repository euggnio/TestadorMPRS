package com.testeBanda.testador.service;

import ch.qos.logback.core.util.FixedDelay;
import com.testeBanda.testador.api.SnmpWanMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @Autowired
    private ScanService scan;

    @Scheduled(cron = "${schedule.testeDiario}")
    public void schedulerTest() {
        log.info("Iniciando teste diário de banda");
        testerService.iniciarTeste("");
    }

    @Scheduled(cron = "${schedule.testeFalhas}")
    public void schedulerTestFalhas(){
        log.info("Iniciando segundo teste de banda para unidades que falharam o teste diário");
        testerService.ligarTesteFalhas();
        testerService.iniciarTeste("");
    }

    @Scheduled(cron = "${schedule.revisaoQuedas}")
    public void revisaTodasQuedas() {
        log.info("Revisando todas as quedas do banco com o Nagios");
        quedaService.revisaTodasQuedas();
    }

    @Scheduled(fixedDelayString = "${schedule.atualizacaoQuedasMs}")
    public void schedulerQuedas(){
        log.debug("ATUALIZANDO QUEDAS");
        quedaService.atualizaQuedas();
    }

    @Scheduled(fixedDelayString = "${snmp.tempoCiclo}")
    private void rodarSnmp(){
        log.debug(" == ATUALIZANDO SNMP == ");
        monitor.setHostLoss();
        monitor.run();
    }

    @Scheduled(cron = "${schedule.varreduraRede}")
    public void rodarScan() {
        log.info("Iniciando scan de rede diário");
        scan.varrerCidades();
    }



}
