package com.testeBanda.testador.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Getter
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

    public String nomeSistema;
    public String cacti;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "cidade")
    public List<Resultados> resultados;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "cidade")
    public List<Contato> contatos;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getIp() {
        return ip;
    }

    public String getVelocidade() {
        return velocidade;
    }

    public void setUltimoTesteBanda(String ultimoTesteBanda) {
        this.ultimoTesteBanda = ultimoTesteBanda;
    }

    public void setCheckTesteBanda(boolean checkTesteBanda) {
        this.checkTesteBanda = checkTesteBanda;
    }

    public List<Resultados> getResultados() {
        return resultados;
    }

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
                ", \"Nome sistema\":\"" + nomeSistema + " \" " +
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
