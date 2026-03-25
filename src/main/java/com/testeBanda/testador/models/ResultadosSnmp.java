package com.testeBanda.testador.models;

import lombok.Getter;

import java.util.Stack;


@Getter
public class ResultadosSnmp {

    private String ip;
    private int velocidade;
    private Stack<double[]> dados = new Stack<>();
    private double rx;
    private double tx;

    public ResultadosSnmp(String ip, int velocidade) {
        this.ip = ip;
        this.velocidade = velocidade;
    }

    public void adicionaDados(double[] valores) {
        this.dados.add(valores);

    }

    public void somarDados() {
        rx = Math.round(dados.stream().mapToDouble(d -> d[0]).average().orElse(0) * 100.0) / 100.0;
        tx = Math.round(dados.stream().mapToDouble(d -> d[1]).average().orElse(0) * 100.0) / 100.0;
    }

    public void removerDados() {
        this.dados.removeFirst();
    }
}
