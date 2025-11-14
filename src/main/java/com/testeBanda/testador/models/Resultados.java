package com.testeBanda.testador.models;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Resultados {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private LocalDateTime dataTeste;
    private String resultado;
    @ManyToOne
    private Cidades cidade;

    public Resultados(LocalDateTime dataTeste  , String resultado, Cidades cidades) {
        this.cidade = cidades;
        this.dataTeste = dataTeste;
        this.resultado = resultado;
    }

    public String getDia(){
        return this.dataTeste.getDayOfMonth() + "/" + this.dataTeste.getMonthValue();

    }
}
