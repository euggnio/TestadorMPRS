package com.testeBanda.testador.service;

import com.testeBanda.testador.api.Microtik;
import com.testeBanda.testador.utils.DataAboutTest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TesterService {

    private DataAboutTest dataAboutTest;
    private Microtik microtik;

    @Autowired
    public TesterService(Microtik microtik, DataAboutTest dataAboutTest) {
        this.microtik = microtik;
        this.dataAboutTest = dataAboutTest;
    }

    public void iniciarTeste(String cidade) {
        log.info("TESTE INICIADO");
        if ( dataAboutTest.isFlag()) {
            microtik.desligar = true;
            if (!microtik.desligar) {
                dataAboutTest.setFlag(false);
            }
            log.info(dataAboutTest.isFlag() ? "DESLIGANDO TESTE" : "TENTATIVA DE INICIAR NOVO TESTE BLOQUEADA, JÁ HÁ TESTES SENDO EFETUADOS " + dataAboutTest.isFlag());
        } else {
            dataAboutTest.setFlag(true);
            microtik.testar(cidade);
            log.info("TESTE FINALIZADO");
            dataAboutTest.setFlag(false);
        }
    }

    public void ligarTesteFalhas(){
        microtik.testarFalha = true;
    }

    public boolean getTesteStatus(){
        return dataAboutTest.isFlag();
    }


}
