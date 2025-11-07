package com.testeBanda.testador.service;

import com.testeBanda.testador.models.Alerta;
import com.testeBanda.testador.models.Queda;
import com.testeBanda.testador.repository.QuedaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

@Service
public class QuedaService{

    @Autowired
    private QuedaRepository quedaRepository;
    private final NagiosAPI nagiosAPI;

    public QuedaService(NagiosAPI nagiosAPI) {
        this.nagiosAPI = nagiosAPI;
    }

    public void editaFaltaDeLuz(long id, boolean novoValor){
        if(quedaRepository.findById(id).isPresent()){
            Queda queda = quedaRepository.findById(id).get();
            queda.setFaltaDeLuz(novoValor);
            quedaRepository.save(queda);
        }
    }

    public List<Queda> findQuedasNoBanco(){
        List<Queda> todasQuedas = quedaRepository.findAll();
        Collections.reverse(todasQuedas);
        return todasQuedas;
    }

    public void atualizaQuedas(){
        List<Queda> quedasNoNagios = getQuedas();
        List<Queda> quedasNoBanco = quedaRepository.findAll();

        if(quedasNoBanco.isEmpty()){
            quedaRepository.saveAll(quedasNoNagios);
        }else{
            Queda ultimaQueda = quedasNoBanco.getLast();
            List<Queda> quedasRecentes = filterQuedasAposData(quedasNoNagios, ultimaQueda.getData().minusWeeks(1));

            boolean match;
            for(Queda quedaRecente : quedasRecentes){
                match = false;
                for(Queda quedaBanco : quedasNoBanco){
                    if(comparaQuedas(quedaRecente, quedaBanco)){
                        match = true;
                        //quedas que estavam sem UP, recebem tempo de duração
                        if(quedaBanco.getTempoFora() == Duration.ZERO && quedaRecente.getTempoFora() != Duration.ZERO){
                            quedaBanco.setTempoFora(quedaRecente.getTempoFora());
                            quedaRepository.save(quedaBanco);
                        }
                    }
                }
                if(!match){quedaRepository.save(quedaRecente);}
            }
        }
    }

    public List<LocalDate> listaDeDatas(List<Queda> quedas){
        Set<LocalDate> setDeDatas = new HashSet<>();

        for(Queda queda : quedas){
            setDeDatas.add(queda.getData().toLocalDate());
        }
        List<LocalDate> ListaDeDatas = new ArrayList<>(setDeDatas);
        ListaDeDatas.sort((LocalDate a, LocalDate b) -> -a.compareTo(b)); //ordem decrescente

        return ListaDeDatas;
    }

    public List<Queda> filterQuedasPorMes(List<Queda> quedas, Month mes){
        return quedas.stream().filter( queda -> queda.getData().getMonth().equals(mes)).toList();
    }

    public List<Queda> filterQuedasPorDia(List<Queda> quedas, LocalDate data){
        return quedas.stream().filter( queda -> queda.getData().toLocalDate().equals(data)).toList();
    }


    private List<Queda> getQuedas(){
        List<Queda> todasQuedas = listaDeQuedas(separaAlertasPorCidade(nagiosAPI.todosAlertasDoAno()));

        sortQuedasPorData(todasQuedas);

        return todasQuedas;
    }

    private boolean comparaQuedas(Queda a, Queda b){
        return a.getCidade().equals(b.getCidade()) && a.getData().equals(b.getData());
    }

    private List<Queda> filterQuedasAposData(List<Queda> quedas, LocalDateTime dataDeCorte){
        return quedas.stream().filter( queda -> queda.getData().isAfter(dataDeCorte)).toList();
    }

    private void sortQuedasPorData(List<Queda> quedas){
        quedas.sort((Queda a, Queda b) -> {
            if (a.getData().isAfter(b.getData())) {
                return 1;
            }if (a.getData().isBefore(b.getData())) {
                return -1;
            } else {
                return 0;
            }
        });
    }

    private Map<String, ArrayList<Alerta>> separaAlertasPorCidade(List<Alerta> alertas){
        Map<String,ArrayList<Alerta>> mapaAlerta = new HashMap<>();
        for (Alerta item : alertas) {
            String cidade = item.getNome();

            mapaAlerta.putIfAbsent(cidade, new ArrayList<>());
            mapaAlerta.get(cidade).add(item);
        }

        return mapaAlerta;
    }

    private List<Queda> listaDeQuedas(Map<String,ArrayList<Alerta>> alertasPorCidades){

        List<Queda> todasQuedas = new ArrayList<>();
        Deque<Alerta> pilha = new ArrayDeque<>();
            for (ArrayList<Alerta> alertasDaCidade : alertasPorCidades.values()) {
                pilha.clear();

                for(Alerta alerta : alertasDaCidade){
                    if(alerta.getTipo().contains("DOWN")){
                        pilha.push(alerta);
                    }
                    if(alerta.getTipo().contains("UP")){
                        if(!pilha.isEmpty()){
                            Alerta down = pilha.pop();
                            Duration duration = Duration.between(down.getData(), alerta.getData());
                            Queda queda = new Queda(alerta.getNome(), down.getData(), duration);
                            todasQuedas.add(queda);
                        }
                    }
                }
                //quedas acontecendo no momento são adicionadas com duração zero
                for(Alerta alerta : pilha){
                    if(alerta.getTipo().contains("DOWN") && !alerta.getNome().isEmpty()){
                        Duration duration = Duration.ZERO;
                        Queda queda = new Queda(alerta.getNome(), alerta.getData(), duration);
                        todasQuedas.add(queda);
                    }
                }
            }
        return todasQuedas;
    }


}
