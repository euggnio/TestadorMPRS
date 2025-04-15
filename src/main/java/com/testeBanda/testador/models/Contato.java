package com.testeBanda.testador.models;

import jakarta.persistence.*;

@Entity
public class Contato {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long id;
    public String nome;
    public String telefone;

    @ManyToOne
    public Cidades cidade;



}
