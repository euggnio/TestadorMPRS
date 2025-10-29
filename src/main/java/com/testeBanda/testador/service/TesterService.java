package com.testeBanda.testador.service;


import com.testeBanda.testador.utils.DataAboutTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TesterService {

    //Classe com função de gerenciar o inicio do teste.

    private DataAboutTest dataAboutTest;
    private Microtik microtik;

    @Autowired
    public TesterService(Microtik microtik, DataAboutTest dataAboutTest) {
        this.microtik = microtik;
        this.dataAboutTest = dataAboutTest;
    }

    public void iniciarTeste(String cidade) {
        System.out.println("TESTE INICIANDO");
        if ( dataAboutTest.isFlag()) {
            microtik.desligar = true;
            if (!microtik.desligar) {
                dataAboutTest.setFlag(false);
            }
            System.out.println(dataAboutTest.isFlag() ? "DESLIGANDO TESTE" : "TENTATIVA DE INICIAR NOVO TESTE BLOQUEADA, JÁ HÁ TESTES SENDO EFETUADOS " + dataAboutTest.isFlag());
        } else {
            System.out.println(" -- Teste inicializado -- ");
            dataAboutTest.setFlag(true);
            microtik.testar(cidade);
            System.out.println("-- Teste finalizado, liberado testes -- ");
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
