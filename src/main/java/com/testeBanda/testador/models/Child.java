package com.testeBanda.testador.models;

import com.testeBanda.testador.models.enums.CategoriaChilds;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Child {
    @Id
    private String nome;
    @ManyToOne
    private Cidades cidade;
    private CategoriaChilds categoria;






}
