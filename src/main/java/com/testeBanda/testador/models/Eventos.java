package com.testeBanda.testador.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Eventos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String descricao;


    public Eventos(String nome, LocalDate localDate, LocalDate localDate1, String descricao) {
        this.nome = nome;
        this.dataInicio = localDate;
        this.dataFim = localDate1;
        this.descricao = descricao;
    }
}
