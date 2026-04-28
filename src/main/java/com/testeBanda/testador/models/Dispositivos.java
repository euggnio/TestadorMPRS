package com.testeBanda.testador.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@Table(name = "dispositivos", indexes = {
        @Index(name = "idx_ip_dispositivo", columnList = "ip", unique = true)
})
public class Dispositivos {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    public Dispositivos(String ip, String nome, Cidades cidade) {
        this.ip = ip;
        this.nome = nome;
        this.cidade = cidade;

    }
    public Dispositivos(String ip, String nome, String descricao,String usuario) {
        this.ip = ip;
        this.nome = nome;
        this.descricao = descricao;
        this.usuario = usuario;

    }    public Dispositivos() {

    }

    @Column(nullable = false)
    private String ip;
    private String nome;
    private String descricao;
    private String usuario;
    private LocalDate dataDaVarredura;
    private LocalDate ultimaVarredura;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "cidade_nome")
    private Cidades cidade;

    @JsonProperty("cidadeNome")
    public String getCidadeNome() {
        return cidade != null ? cidade.getNome() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dispositivos that = (Dispositivos) o;
        return Objects.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip);
    }
}
