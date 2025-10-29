package com.testeBanda.testador.utils;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class DataAboutTest {
    //Classe responsável pelos dados atomicos, para enviar para o front como se está sendo testado é a cidade atual em teste.

    private final AtomicBoolean flag = new AtomicBoolean(false);
    private final AtomicReference<String> cidadeEmTeste = new AtomicReference<>();

    public DataAboutTest() {
        this.cidadeEmTeste.set("@END");
    }

    public boolean isFlag() {
        return flag.get();
    }

    public void setFlag(boolean value) {
        flag.set(value);
    }

    public String getCidadeEmTeste() {
        return cidadeEmTeste.get();
    }

    public void setCidadeEmTeste(String cidade) {
        cidadeEmTeste.set(cidade);
    }}