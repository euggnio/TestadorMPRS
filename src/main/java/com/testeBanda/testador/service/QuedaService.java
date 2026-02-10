package com.testeBanda.testador.service;

import com.testeBanda.testador.DTO.DadosAlertaDTO;
import com.testeBanda.testador.api.CheckMKAPI;
import com.testeBanda.testador.api.GlpiAPI;
import com.testeBanda.testador.api.NagiosAPI;
import com.testeBanda.testador.models.Alerta;
import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.models.Queda;
import com.testeBanda.testador.repository.QuedaRepository;
import com.testeBanda.testador.utils.Calculos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class QuedaService{

    @Autowired
    private QuedaRepository quedaRepository;
    private final CidadeService cidadeService;
    private final NagiosAPI nagiosAPI;
    private final CheckMKAPI checkMKAPI;
    private final GlpiAPI glpiAPI;

    public QuedaService(NagiosAPI nagiosAPI, CheckMKAPI checkMKAPI, CidadeService cidadeService, GlpiAPI glpiAPI) {
        this.nagiosAPI = nagiosAPI;
        this.checkMKAPI = checkMKAPI;
        this.cidadeService = cidadeService;
        this.glpiAPI = glpiAPI;
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

    public void sincronizarNomesCidades(){
        List<Queda> quedas = getQuedas();
        List<Cidades> cidades = cidadeService.findAll();
        for (Cidades cidade : cidades) {
            Optional<Queda> primeiraQueda = quedas.stream()
                    .filter(queda -> Calculos.nomesIguais(cidade.nome, queda.getNomeCidade()))
                    .findFirst(); // <-- para no primeiro que encontrarprimeiraQueda
            if(primeiraQueda.isPresent()){
                cidade.nagiosID = primeiraQueda.get().getNomeCidade();
                cidadeService.salvarCidade(cidade);
            }
            //TODO Else para se n encontrou correspondencia?
        }
    }

    public void sincronizarQuedasComCidade(List<Queda> quedas){
        List<Cidades> cidades = cidadeService.findAll();
        for (Cidades cidade : cidades) {
            for (Queda q : quedas){
                if(q.getNomeCidade().equals(cidade.nagiosID)){
                    q.setCidade(cidade);
                }
            }
        }
    }

    public void salvarQuedas(List<Queda> quedas){
        List<Cidades> cidades = cidadeService.findAll();
        for (Queda queda : quedas) {
            for (Cidades cidade : cidades) {
                if( Objects.equals(cidade.nagiosID, queda.getNomeCidade()) ){
                    queda.setCidade(cidade);
                }
            }
        }
        quedaRepository.saveAll(quedas);
    }

    public void atualizaQuedas(){
        List<Queda> quedasNoBanco = quedaRepository.findAll();

        //Verificação inicial, preenchendo o banco de dados. roda uma unica vez
        //TODO criar um metodo para isso, metodos de
        // configuração devem ser estaticos, rodar com verificações e proibir o sistema de rodar em caso de falha.
        if(quedasNoBanco.isEmpty())
        {
            List<Queda> todasQuedasNoNagios = getQuedasDesde2023(); // se estiver vazio pega o historico completo do nagios
            sincronizarNomesCidades();
            salvarQuedas(todasQuedasNoNagios);
        }
        else{
            LocalDateTime dataUltimaQueda = quedasNoBanco.getLast().getData().minusWeeks(1);

            List<Queda> quedasNoNagios = getQuedas();
            List<Queda> quedasRecentes = filterQuedasAposData(quedasNoNagios, dataUltimaQueda);
            sincronizarQuedasComCidade(quedasRecentes);
            processaNovasQuedas(quedasRecentes, filterQuedasAposData(quedasNoBanco, dataUltimaQueda.minusMonths(1)));
        }
    }

    /** Calcula tempo fora para quedas que estavam DOWN até a última atualização */
    private void processaNovasQuedas(List<Queda> quedasRecentes, List<Queda> quedasNoBanco){
        boolean match;
        for(Queda quedaRecente : quedasRecentes){
            match = false;
            for(Queda quedaBanco : quedasNoBanco){
                if(comparaQuedas(quedaRecente, quedaBanco)){
                    match = true;

                    if(quedaBanco.getTempoFora() == Duration.ZERO && quedaBanco.getChamado().isBlank()){
                        Duration tempoDaQueda = Duration.between( quedaBanco.getData(), LocalDateTime.now());
                        String ticket = glpiAPI.createGlpiTicket(quedaBanco.getNomeCidade());
                        quedaBanco.setChamado(ticket);
                        if ( !ticket.isBlank() ){
                            quedaRepository.save(quedaBanco);
                        }
                    }
                    // quedas que estavam sem UP, recebem tempo de duração
                    if(quedaBanco.getTempoFora() == Duration.ZERO && quedaRecente.getTempoFora() != Duration.ZERO){
                        quedaBanco.setTempoFora(quedaRecente.getTempoFora());
                        quedaBanco.setUptime(quedaRecente.getUptime());
                        quedaRepository.save(quedaBanco);
                    }
                }
            }
            if(!match){
                quedaRepository.save(quedaRecente);
            }
        }
    }

//    public Long getUptime(Queda queda){
//        if(queda.getCidade() == null){
//            return -2L;
//        }
//        else{
//            Long tempo = checkMKAPI.getUptimePosQueda(queda, 0);
//
//            int offset = 0;
//            while(tempo <= 0 && offset < 7){
//                offset++;
//                System.out.println("offset:" + offset);
//                tempo = checkMKAPI.getUptimePosQueda(queda, offset);
//            }
//
//            System.out.println("offset final: " + offset);
//
//            System.out.println("Uptime em " + queda.getNomeCidade() + " " + queda.getData() + ": " + tempo);
//            return tempo;
//        }
//    }

    public List<LocalDate> listaDeDatas(List<Queda> quedas){
        Set<LocalDate> setDeDatas = new HashSet<>();

        for(Queda queda : quedas){
            setDeDatas.add(queda.getData().toLocalDate());
        }
        List<LocalDate> ListaDeDatas = new ArrayList<>(setDeDatas);
        ListaDeDatas.sort((LocalDate a, LocalDate b) -> -a.compareTo(b)); //ordem decrescente

        return ListaDeDatas;
    }

    public List<Queda> filterQuedasPorMes(List<Queda> quedas, int ano, Month mes){
        return quedas.stream().filter( queda -> queda.getData().getMonth().equals(mes) && queda.getData().getYear() == ano).toList();
    }

    public List<Queda> filterQuedasPorDia(List<Queda> quedas, LocalDate data){
        return quedas.stream().filter( queda -> queda.getData().toLocalDate().equals(data)).toList();
    }

    private List<Queda> getQuedas(){
        List<Queda> todasQuedas = listaDeQuedas(separaAlertasPorCidade(nagiosAPI.todosAlertasDoAno()));
        sortQuedasPorData(todasQuedas);

        return todasQuedas;
    }

    private List<Queda> getQuedasDesde2023(){
        List<Queda> todasQuedas = listaDeQuedas(separaAlertasPorCidade(nagiosAPI.todosAlertasDesde2023()));
        sortQuedasPorData(todasQuedas);

        return todasQuedas;
    }

    private boolean comparaQuedas(Queda a, Queda b){
        return a.getNomeCidade().equals(b.getNomeCidade()) && a.getData().equals(b.getData());
    }

    public List<Queda> filterQuedasAposData(List<Queda> quedas, LocalDateTime dataDeCorte){
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

    private List<Integer> listaDeAnos(List<Queda> quedas){
        Set<Integer> setQ = new HashSet<>();
        for(Queda q : quedas){
            setQ.add(q.getData().getYear());
        }
        return new ArrayList<>(setQ);
    }

    public DadosAlertaDTO PreencherDTO(DadosAlertaDTO dto, int ano) {
        List<Queda> quedas = quedaRepository.findAll();
        dto.mesDisponibilidades = nagiosAPI.relatorioDeDisponibilidade(ano);
        dto.quedas = quedas.stream().filter(q -> q.getData().getYear() == ano).toList();
        dto.listaAnos = listaDeAnos(quedas);
        return dto;
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
                        Queda queda = new Queda(alerta.getNome(), down.getData(), duration, alerta.getUptime());
                        todasQuedas.add(queda);
                    }
                }
            }
            //quedas acontecendo no momento são adicionadas com duração zero
            for(Alerta alerta : pilha){
                if(alerta.getTipo().contains("DOWN") && !alerta.getNome().isEmpty()){
                    System.out.println(alerta.toString());
                    Duration duration = Duration.ZERO;
                    Queda queda = new Queda(alerta.getNome(), alerta.getData(), duration, 0L);
                    todasQuedas.add(queda);
                }
            }
        }
        return todasQuedas;
    }

    public void editarProtocolo(long id, String protocolo) {
        Queda queda = quedaRepository.findById(id).get();
        queda.setProtocolo(protocolo);
        quedaRepository.save(queda);
        if (!queda.getChamado().isBlank()){
            glpiAPI.insertFollowUpTicket(queda.getChamado(), "Protocolo ávato : " + protocolo);
        }
    }

    public void adicionarFolloyUp(long id, String texto) {
        Queda queda = quedaRepository.findById(id).get();
        if(queda.getChamado().isBlank()){
            return;
        }
        glpiAPI.insertFollowUpTicket(queda.getChamado(), "From testador: " + texto);
    }

    public void fecharChamado(long id, String texto) {
        Queda queda = quedaRepository.findById(id).get();
        if(queda.getChamado().isBlank()){
            System.out.println("Chamado nao encontrado");
            return;
        }
        glpiAPI.insertFollowUpTicket(queda.getChamado(), "Chamado fechado pelo testador, protocolo: " + queda.getProtocolo()
        + " chamado: " + queda.getChamado() + " tempo fora: " + queda.getTempoFora() + " queda de energia? " + (queda.isFaltaDeLuz()? "Sim" : "nao"));
        glpiAPI.closeGlpiTicket(queda.getChamado());
    }




}
