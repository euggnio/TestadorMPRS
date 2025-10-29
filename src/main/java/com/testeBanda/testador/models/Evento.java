package com.testeBanda.testador.models;

import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Evento {
    public String tombo;
    public String hora;
    public String usuario;
    public String mensagem;

    public LocalDateTime getTimeCompleto(){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
        return LocalDateTime.parse(this.hora, formatter);
    }

}