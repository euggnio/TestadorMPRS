package com.testeBanda.testador.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.util.Stack;


@Getter
public class ResultadosSnmp {

    private String ip;
    private int velocidade;
    private Stack<double[]> dados = new Stack<>();
    private double rx;
    private double tx;
    private double loss;
    private String smokeID;
    private String interfaceWan;
    private String interfaceLan;

    public ResultadosSnmp(String ip, int velocidade, String smokeID, String interfaceWan, String interfaceLan) {
        this.smokeID = smokeID;
        this.ip = ip;
        this.velocidade = velocidade;
        this.interfaceWan = interfaceWan;
        this.interfaceLan = interfaceLan;
    }

    public void adicionaDados(double[] valores) {
        this.dados.add(valores);

    }

    public void adicionarLoss(double loss ){
        this.loss = loss;
    }

    public void somarDados() {
        rx = Math.round(dados.stream().mapToDouble(d -> d[0]).average().orElse(0) * 100.0) / 100.0;
        tx = Math.round(dados.stream().mapToDouble(d -> d[1]).average().orElse(0) * 100.0) / 100.0;
    }

    public void removerDados() {
        this.dados.removeFirst();
    }

    @JsonIgnore
    public int getterInterfaceWanID(){
        return Integer.parseInt(this.interfaceWan);
    }
}
