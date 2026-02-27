package com.testeBanda.testador.service;

import com.testeBanda.testador.DTO.DadosAlertaDTO;
import com.testeBanda.testador.api.GlpiAPI;
import com.testeBanda.testador.api.NagiosAPI;
import com.testeBanda.testador.models.Alerta;
import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.models.Queda;
import com.testeBanda.testador.repository.QuedaRepository;
import com.testeBanda.testador.utils.Calculos;
import com.testeBanda.testador.utils.QuedaUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

@Slf4j
@Service
public class QuedaService {

    @Autowired
    private final QuedaRepository quedaRepository;
    private final CidadeService cidadeService;
    private final NagiosAPI nagiosAPI;
    private final GlpiService glpiService;
    private final QuedaUtils quedaUtils;


    public QuedaService(NagiosAPI nagiosAPI, CidadeService cidadeService, GlpiAPI glpiAPI, QuedaRepository quedaRepository, GlpiService glpiService, QuedaUtils quedaUtils) {
        this.nagiosAPI = nagiosAPI;
        this.cidadeService = cidadeService;
        this.quedaRepository = quedaRepository;
        this.glpiService = glpiService;
        this.quedaUtils = quedaUtils;
    }

    public void editaFaltaDeLuz(long id, boolean novoValor) {
        if (quedaRepository.findById(id).isPresent()) {
            Queda queda = quedaRepository.findById(id).get();
            queda.setFaltaDeLuz(novoValor);
            log.info("Alterando energia na queda: {}", queda);
            quedaRepository.save(queda);
        }
    }

    public List<Queda> findQuedasNoBanco() {
        List<Queda> todasQuedas = quedaRepository.findAll();
        Collections.reverse(todasQuedas);

        return todasQuedas;
    }

    public List<Queda> findQuedasDoDia(LocalDate data){
        List<Queda> todasQuedas = findQuedasNoBanco();
        quedaUtils.quedasEmAndamentoPrimeiro(todasQuedas);

        return quedaUtils.filterQuedasPorDia(todasQuedas, data);
    }

    public List<Queda> findQuedasDoMes(int ano, Month mes){
        List<Queda> todasQuedas = findQuedasNoBanco();
        quedaUtils.quedasEmAndamentoPrimeiro(todasQuedas);

        return quedaUtils.filterQuedasPorMes(todasQuedas, ano, mes);
    }

    public List<LocalDate> findListaDatas(){
        List<Queda> todasQuedas = quedaRepository.findAll();

        return quedaUtils.listaDeDatas(todasQuedas);
    }

    public void sincronizarNomesCidades(List<Queda> quedas) {
        List<Cidades> cidades = cidadeService.findAll();
        for (Cidades cidade : cidades) {
            Optional<Queda> primeiraQueda = quedas.stream()
                    .filter(queda -> Calculos.nomesIguais(cidade.nome, queda.getNomeCidade()))
                    .findFirst(); // <-- para no primeiro que encontrarprimeiraQueda
            if (primeiraQueda.isPresent()) {
                cidade.nagiosID = primeiraQueda.get().getNomeCidade();
                cidadeService.salvarCidade(cidade);
            }
            //TODO Else para se n encontrou correspondencia?
        }
    }

    public void sincronizarQuedasComCidade(List<Queda> quedas) {
        List<Cidades> cidades = cidadeService.findAll();
        for (Cidades cidade : cidades) {
            for (Queda q : quedas) {
                if (q.getNomeCidade().equals(cidade.nagiosID)) {
                    q.setCidade(cidade);
                }
            }
        }
    }

    private void popularBancoVazio() {
        log.info("Banco vazio, coletando todo histórico de quedas do nagios");
        List<Queda> todasQuedasNoNagios = getQuedasDesde2023(); // se estiver vazio pega o historico completo do nagios

        sincronizarNomesCidades(todasQuedasNoNagios);
        sincronizarQuedasComCidade(todasQuedasNoNagios);

        quedaRepository.saveAll(todasQuedasNoNagios);
    }

    /** Fecha quedas mais antigas que uma semana */
    public void revisaTodasQuedas() {
        List<Queda> quedasNoBanco = quedaRepository.findAll();
        List<Queda> quedasNoNagios = getQuedas();

        sincronizarQuedasComCidade(quedasNoNagios);

        processaNovasQuedas(quedasNoNagios, quedasNoBanco);
    }

    /** Roda a cada minuto para pegar quedas do nagios */
    public void atualizaQuedas() {
        List<Queda> quedasNoBanco = quedaRepository.findAll();

        if (quedasNoBanco.isEmpty()) {
            popularBancoVazio();
            return;
        }
        LocalDateTime dataDeCorte = quedasNoBanco.getLast().getData().minusWeeks(1);

        List<Queda> quedasRecentes = quedaUtils.filterQuedasAposData(getQuedas(), dataDeCorte);
        List<Queda> quedasDoBancoRecentes = quedaUtils.filterQuedasAposData(quedasNoBanco, dataDeCorte);

        sincronizarQuedasComCidade(quedasRecentes);

        processaNovasQuedas(quedasRecentes, quedasDoBancoRecentes);
    }

    /** Calcula tempo fora para quedas que estavam DOWN até a última atualização */
    private void processaNovasQuedas(List<Queda> quedasRecentes, List<Queda> quedasNoBanco) {
        boolean match;
        for (Queda quedaRecente : quedasRecentes) {
            match = false;
            for (Queda quedaBanco : quedasNoBanco) {
                if (quedaUtils.comparaQuedas(quedaRecente, quedaBanco)) {
                    match = true;

                    resolveQueda(quedaBanco, quedaRecente); // quedas que estavam sem UP, recebem tempo de duração
                    abreGLPI(quedaBanco, quedaRecente);   // abre GLPI para quedas em andamento com mais de 10min
                }
            }
            if (!match) {
                quedaRepository.save(quedaRecente);
                log.info("Salvando nova queda no banco: {}", quedaRecente);
            }
        }
    }

    private void abreGLPI(Queda quedaBanco, Queda quedaRecente) {
        if (quedaBanco.getTempoFora() == Duration.ZERO && quedaBanco.getChamado().isBlank()) {
            Duration tempoDaQueda = Duration.between(quedaBanco.getData(), LocalDateTime.now());
            if (tempoDaQueda.toSeconds() > 600) {
                glpiService.abrirChamado(quedaBanco.getId());
            }
        }
    }

    private void resolveQueda(Queda quedaBanco, Queda quedaRecente) {
        if (quedaBanco.getTempoFora() == Duration.ZERO && quedaRecente.getTempoFora() != Duration.ZERO) {
            quedaBanco.setTempoFora(quedaRecente.getTempoFora());
            quedaBanco.setUptime(quedaRecente.getUptime());
            log.info("Resolvendo queda com TempoFora e Uptime: {}", quedaBanco);
            if (!quedaBanco.getChamado().isBlank()) {
                glpiService.fecharChamado(quedaBanco.getId(), "");
            }
            quedaRepository.save(quedaBanco);
        }
    }

    /** Pega quedas do ano atual no Nagios */
    private List<Queda> getQuedas() {
        List<Queda> todasQuedas = listaDeQuedas(separaAlertasPorCidade(nagiosAPI.todosAlertasDoAno()));
        quedaUtils.sortQuedasPorData(todasQuedas);

        return todasQuedas;
    }

    private List<Queda> getQuedasDesde2023() {
        List<Queda> todasQuedas = listaDeQuedas(separaAlertasPorCidade(nagiosAPI.todosAlertasDesde2023()));
        quedaUtils.sortQuedasPorData(todasQuedas);

        return todasQuedas;
    }

    private Map<String, ArrayList<Alerta>> separaAlertasPorCidade(List<Alerta> alertas) {
        Map<String, ArrayList<Alerta>> mapaAlerta = new HashMap<>();
        for (Alerta item : alertas) {
            String cidade = item.getNome();
            mapaAlerta.putIfAbsent(cidade, new ArrayList<>());
            mapaAlerta.get(cidade).add(item);
        }

        return mapaAlerta;
    }

    private List<Queda> listaDeQuedas(Map<String, ArrayList<Alerta>> alertasPorCidades) {
        List<Queda> todasQuedas = new ArrayList<>();
        Deque<Alerta> pilha = new ArrayDeque<>();

        for (ArrayList<Alerta> alertasDaCidade : alertasPorCidades.values()) {
            pilha.clear();

            for (Alerta alerta : alertasDaCidade) {
                if (alerta.getTipo().contains("DOWN")) {
                    pilha.push(alerta);
                }
                if (alerta.getTipo().contains("UP")) {
                    if (!pilha.isEmpty()) {
                        Alerta down = pilha.pop();
                        Duration duration = Duration.between(down.getData(), alerta.getData());
                        Queda queda = new Queda(alerta.getNome(), down.getData(), duration, alerta.getUptime());
                        todasQuedas.add(queda);
                    }
                }
            }
            //quedas acontecendo no momento são adicionadas com duração zero
            for (Alerta alerta : pilha) {
                if (alerta.getTipo().contains("DOWN") && !alerta.getNome().isEmpty()) {
                    Duration duration = Duration.ZERO;
                    Queda queda = new Queda(alerta.getNome(), alerta.getData(), duration, 0L);
                    todasQuedas.add(queda);
                }
            }
        }
        return todasQuedas;
    }

    /** Dados para a página de gráficos */
    public DadosAlertaDTO PreencherDTO(DadosAlertaDTO dto, int ano) {
        List<Queda> quedas = quedaRepository.findAll();
        dto.mesDisponibilidades = nagiosAPI.relatorioDeDisponibilidade(ano);
        dto.quedas = quedas.stream().filter(q -> q.getData().getYear() == ano).toList();
        dto.listaAnos = quedaUtils.listaDeAnos(quedas);
        return dto;
    }

}
