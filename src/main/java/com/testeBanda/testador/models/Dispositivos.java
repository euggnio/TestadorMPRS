package com.testeBanda.testador.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@ToString
public class Dispositivos {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    private String ip;
    private String nome;
    private String descricao;

    @ManyToOne
    @JoinColumn(name = "cidade_nome")
    private Cidades cidade;

}
