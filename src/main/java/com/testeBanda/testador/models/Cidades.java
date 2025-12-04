package com.testeBanda.testador.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
public class Cidades {

    @Id
    public String nome;
    public String codigo;
    public String intra;
    public String ip;
    public String velocidade;
    public boolean checkTesteBanda;
    @Size(min = 1, max = 500)
    public String ultimoTesteBanda;
    public String dataUltimoTeste;
    //Ids dos hosts em todos os programas são o nome do host...
    public String smokeID;
    public String cacti;
    public String nagiosID;
    public String checkMKid;


    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "cidade")
    public List<Resultados> resultados;

    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "cidade")
    public List<Contato> contatos;

    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = false, mappedBy = "cidade")
    public List<Queda> quedas;


    @Override
    public String toString() {
        return "{" +
                "\"nome\":\"" + nome + "\"" +
                ", \"codigo\":\"" + codigo + " \" " +
                ", \"intra\":\"" + intra + " \" " +
                ", \"ip\":\"" + ip + " \" " +
                ", \"velocidade\":\"" + velocidade + " \" " +
                ", \"checkTesteBanda\":" + checkTesteBanda +
                ", \"ultimoTesteBanda\":\"" + ultimoTesteBanda + " \" " +
                ", \"Nome sistema\":\"" + smokeID + " \" " +
                "}";

    }

    public int getVelocidadeInteger(){
        String inteiros = velocidade.replaceAll("\\D", "");
        return Integer.parseInt(inteiros);
    }

    public void addResultado(LocalDateTime data, String resultado){
        this.resultados.add(new Resultados(data,resultado,this));

    }


}
