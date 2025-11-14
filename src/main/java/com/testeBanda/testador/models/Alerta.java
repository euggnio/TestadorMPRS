package com.testeBanda.testador.models;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Alerta  {

    private String nome;
    private LocalDateTime data;
    private String tipo;;
    private String tempoFora;

    public Alerta(String nome, LocalDateTime data, String tipo) {
        this.nome = nome;
        this.data = data;
        if(tipo.contains("OK")){
            this.tipo = "UP";
        }else if(tipo.contains("CRITICAL") || tipo.contains("timed out")){
            this.tipo = "DOWN";
        }
        else if(tipo.contains("WARNING")){
            this.tipo = "UP";
        }
        else {
            this.tipo = tipo;
        }
    }

    @Override
    public String toString() {
        return "Alerta{" +
                "nome='" + nome + '\'' +
                ", data=" + data +
                ", tipo='" + tipo + '\'' +
                ", tempoFora='" + tempoFora + '\'' +
                '}';
    }
}