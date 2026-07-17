package com.testeBanda.testador.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.testeBanda.testador.models.configs.ConfigCidades;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
public class Cidades {

    @Id
    public String nome;
    @Size(max = 4)
    public String codigo;
    public String intra;
    public String ip;
    public String velocidade;
    public String coordenadas;
    @Column(name = "notacao", length = 2)
    @Size(min = 2, max = 2)
    public String notacao;
    //Ids dos hosts em todos os programas são o nome do host...
    public String smokeID;
    public String cacti;
    public String nagiosID;

    //Datas
    public LocalDateTime ultimaVarredura;
    @Size(min = 1, max = 500)
    public String ultimoTesteBanda;
    public String dataUltimoTeste;
    //checks
    public boolean checkTesteBanda = false;
    //checks de configs
    public boolean duplaAbordagem = false;
    public boolean bloquearTesteBanda = false;
    public boolean limitarTesteBanda = false;



    @OneToOne(cascade = CascadeType.ALL)
    public ConfigCidades config = new ConfigCidades();

    @PrePersist
    private void garantirConfiguracao() {
        if (config == null) {
            config = new ConfigCidades();
        }
    }

    //RELACIONAMENTOS
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "cidade")
    public List<Dispositivos> dispositivos;

    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "cidade")
    public List<Resultados> resultados;

    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "cidade")
    public List<Child> childs;

    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "cidade")
    public List<Queda> quedas;


    public int getVelocidadeInteger(){
        String inteiros = velocidade.replaceAll("\\D", "");
        return Integer.parseInt(inteiros);
    }

    public void addResultado(LocalDateTime data, String resultado){
        this.resultados.add(new Resultados(data,resultado,this));
    }

}
