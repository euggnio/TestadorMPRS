package com.testeBanda.testador.DTO;

import com.testeBanda.testador.models.Alerta;
import com.testeBanda.testador.models.Mes;
import com.testeBanda.testador.models.Queda;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;


@Getter
public class DadosAlertaDTO {

    public String msgTempoAleras;
    public List<Alerta> alertasDown= new ArrayList<>();
    public List<Queda> quedas= new ArrayList<>();
    public List<Mes> mesDisponibilidades = new ArrayList<>();
    public List<Integer> listaAnos;

}
