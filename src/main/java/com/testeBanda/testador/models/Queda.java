package com.testeBanda.testador.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Duration;
import java.time.LocalDateTime;

import java.util.List;

@Entity
public class Queda {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;
    private String cidade;
    private LocalDateTime data;
    private Duration tempoFora;
    private boolean faltaDeLuz;

    public Queda() {
    }

    public Queda(String cidade, LocalDateTime data, Duration tempoFora) {
        this.cidade = cidade;
        this.data = data;
        this.tempoFora = tempoFora;
    }

    public long getId() {
        return id;
    }

    public String getCidade() {
        return cidade;
    }

    public Duration getTempoFora() {
        return tempoFora;
    }

    public LocalDateTime getData() {
        return data;
    }

    public boolean isFaltaDeLuz() {
        return faltaDeLuz;
    }

    public void setFaltaDeLuz(boolean faltaDeLuz) {
        this.faltaDeLuz = faltaDeLuz;
    }

    public void setTempoFora(Duration tempoFora) {
        this.tempoFora = tempoFora;
    }

    public String toString() {
        return "Queda{" +
                "cidade='" + cidade + '\'' +
                ", data=" + data +
                ", tempoFora='" + tempoFora + '\'' +
                ", faltaDeLuz='" + faltaDeLuz + '\'' +
                "}";
    }


}
