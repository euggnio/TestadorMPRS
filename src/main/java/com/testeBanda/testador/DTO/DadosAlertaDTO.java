package com.testeBanda.testador.DTO;

import com.testeBanda.testador.models.Alerta;
import com.testeBanda.testador.models.Disponibilidade;
import com.testeBanda.testador.models.Mes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class DadosAlertaDTO {

    public String msgTempoAleras;
    public List<Alerta> alertasDown= new ArrayList<>();
    public List<Mes> mesDisponibilidades = new ArrayList<>();

    public List<Mes> getMesDisponibilidades() {
        return mesDisponibilidades;
    }

    public void setMesDisponibilidades(List<Mes> mesDisponibilidades) {
        this.mesDisponibilidades = mesDisponibilidades;
    }

    public List<Alerta> getAlertasDown() {
        return alertasDown;
    }

    public void setAlertasDown(List<Alerta> alertasDown) {
        this.alertasDown = alertasDown;
    }


    public ArrayList<Disponibilidade> getMesDeDisponibilidadeByMonthIndex(int monthIndex) {
        return mesDisponibilidades.stream()
                .filter(mes -> mes.month.getValue() == monthIndex) // filtra pelo mês
                .flatMap(mes -> mes.disponibilidades.stream())     // transforma Mes → Disponibilidades
                .collect(Collectors.toCollection(ArrayList::new)); // coleta em ArrayList
    }



}
