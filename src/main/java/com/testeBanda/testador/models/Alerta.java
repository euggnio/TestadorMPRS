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
    private String tipo;
    private String tempoFora;
    private long uptime;

    public Alerta(String nome, LocalDateTime data, String tipo) {
        this.nome = nome;
        this.data = data;
        if(tipo.contains("OK")){
            this.tipo = "UP";
            this.uptime = coletaUptimeDoAlerta(tipo);
        }
        else if(tipo.contains("CRITICAL") || tipo.contains("timed out")){
            this.tipo = "DOWN";
            this.uptime = 0L;
        }
        else if(tipo.contains("WARNING")){
            this.tipo = "UP";
            this.uptime = coletaUptimeDoAlerta(tipo);
        }
        else {
            this.tipo = tipo;
            this.uptime = 0L;
        }
    }

    private long coletaUptimeDoAlerta(String str){
        if(str.contains("Uptime")){
            String valor_str = str.split(":")[1].trim().replace("s", "");

            return Long.parseLong(valor_str);
        }else{
            return 0L;
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