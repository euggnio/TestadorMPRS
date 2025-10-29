package com.testeBanda.testador.models;

import lombok.Getter;
import lombok.Setter;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Locale;

@Getter
@Setter
public class Mes {

    public Month month;
    public ArrayList<Disponibilidade> disponibilidades;

    public Mes(Month month) {
        this.month = month;
        this.disponibilidades = new ArrayList<>();

    }

    @Override
    public String toString() {
        return "Mes{" +
                "month=" + month.toString() +
                ", disponibilidades=" + disponibilidades.toString() +
                '}';
    }

    public String getNomeMesPtBr() {
        return this.month.getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
    }

}
