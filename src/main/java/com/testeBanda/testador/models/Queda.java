package com.testeBanda.testador.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Queda {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;
    private String nomeCidade;
    private LocalDateTime data;
    private Duration tempoFora;
    private boolean faltaDeLuz;
    private String uptime;

    @Getter
    @ManyToOne
    private Cidades cidade;

    public Queda(String cidade, LocalDateTime data, Duration tempoFora) {
        this.nomeCidade = cidade;
        this.data = data;
        this.tempoFora = tempoFora;
    }

    public LocalDateTime getDataUp(){
        return this.data.plus(tempoFora);
    }

    public String toString() {
        return "Queda{" +
                "cidade='" + nomeCidade + '\'' +
                ", data=" + data +
                ", tempoFora='" + tempoFora + '\'' +
                ", faltaDeLuz='" + faltaDeLuz + '\'' +
                "}";
    }


}
