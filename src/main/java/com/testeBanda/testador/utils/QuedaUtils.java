package com.testeBanda.testador.utils;

import com.testeBanda.testador.models.Queda;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class QuedaUtils {


    public boolean comparaQuedas(Queda a, Queda b) {
        return a.getNomeCidade().equals(b.getNomeCidade()) && a.getData().equals(b.getData());
    }

    public List<Queda> filterQuedasAposData(List<Queda> quedas, LocalDateTime dataDeCorte) {
        return quedas.stream().filter(queda -> queda.getData().isAfter(dataDeCorte)).toList();
    }

    public List<Queda> filterQuedasPorMes(List<Queda> quedas, int ano, Month mes) {
        return quedas.stream().filter(queda -> queda.getData().getMonth().equals(mes) && queda.getData().getYear() == ano).toList();
    }

    public List<Queda> filterQuedasPorDia(List<Queda> quedas, LocalDate data) {
        return quedas.stream().filter(queda -> queda.getData().toLocalDate().equals(data)).toList();
    }

    public void quedasEmAndamentoPrimeiro(List<Queda> quedas){
        // traz quedas atualmente DOWN para o topo da lista
        List<Queda> atuais = new ArrayList<>();
        for (Queda q : quedas) {
            if (q.getTempoFora() == Duration.ZERO) {
                atuais.add(q);
            }
        }
        quedas.removeAll(atuais);
        quedas.addAll(0, atuais);
    }

    public void sortQuedasPorData(List<Queda> quedas) {
        quedas.sort((Queda a, Queda b) -> {
            if (a.getData().isAfter(b.getData())) {
                return 1;
            }
            if (a.getData().isBefore(b.getData())) {
                return -1;
            } else {
                return 0;
            }
        });
    }

    public List<Integer> listaDeAnos(List<Queda> quedas) {
        Set<Integer> setQ = new HashSet<>();
        for (Queda q : quedas) {
            setQ.add(q.getData().getYear());
        }
        return new ArrayList<>(setQ);
    }

    public List<LocalDate> listaDeDatas(List<Queda> quedas) {
        Set<LocalDate> setDeDatas = new HashSet<>();

        for (Queda queda : quedas) {
            setDeDatas.add(queda.getData().toLocalDate());
        }
        List<LocalDate> ListaDeDatas = new ArrayList<>(setDeDatas);
        ListaDeDatas.sort((LocalDate a, LocalDate b) -> -a.compareTo(b)); //ordem decrescente

        return ListaDeDatas;
    }

}
