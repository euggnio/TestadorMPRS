package com.testeBanda.testador.service;

import com.testeBanda.testador.utils.StatusTeste;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SchedulerTeste {

    @Autowired
    private Microtik microtik;
    @Autowired
    private StatusTeste statusTeste;


    @Scheduled(cron = "0 0 0,23 * * ?")
    public void schedulerTest(){
        System.out.println("Scheduler Test");
        if ( statusTeste.isFlag()) {
            microtik.desligar = true;
            if (!microtik.desligar) {
                statusTeste.setFlag(false);
            }
            System.out.println(statusTeste.isFlag() ? "DESLIGANDO TESTE" : "TENTATIVA DE INICIAR NOVO TESTE BLOQUEADA, JÁ HÁ TESTES SENDO EFETUADOS " + statusTeste.isFlag());
        } else {
            System.out.println("TESTE INICIALIZADO BLOQUEANDO TESTE");
            statusTeste.setFlag(true);
            microtik.testarVelocidade(null);
            System.out.println("TESTE FINALIZADO LIBERANDO NOVO TESTE");
            statusTeste.setFlag(false);
        }
    }

}
