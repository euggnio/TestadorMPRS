package com.testeBanda.testador.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Disponibilidade {

    public String nome;
    public long timeUp;
    public long timeDown;
    public double disponibilidade;
    public double indisponibilidade;

    public Disponibilidade(String nome, double disponibilidade, double indisponibilidade) {
        this.nome = nome;
        this.disponibilidade = disponibilidade;
        this.indisponibilidade = indisponibilidade;
    }

    @Override
    public String toString() {
        return "Disponibilidade{" +
                "nome='" + nome + '\'' +
                ", timeUp=" + timeUp +
                ", timeDown=" + timeDown +
                ", disponibilidade=" + disponibilidade +
                ", indisponibilidade=" + indisponibilidade +
                '}';
    }
}
