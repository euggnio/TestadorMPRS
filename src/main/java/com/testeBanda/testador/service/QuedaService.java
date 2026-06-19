package com.testeBanda.testador.service;

import com.testeBanda.testador.DTO.DadosAlertaDTO;
import com.testeBanda.testador.api.GlpiAPI;
import com.testeBanda.testador.api.NagiosAPI;
import com.testeBanda.testador.models.Alerta;
import com.testeBanda.testador.models.CategoriaQueda;
import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.models.Queda;
import com.testeBanda.testador.repository.QuedaRepository;
import com.testeBanda.testador.utils.Calculos;
import com.testeBanda.testador.utils.QuedaUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QuedaService {

    @Autowired
    private final QuedaRepository quedaRepository;
    private final CidadeService cidadeService;
    private final NagiosAPI nagiosAPI;
    private final GlpiService glpiService;
    private final QuedaUtils quedaUtils;
    @Value("${glpi.glpiBlockAbertura}")
    private boolean glpiBloqueado;

    public QuedaService(NagiosAPI nagiosAPI, CidadeService cidadeService, GlpiAPI glpiAPI, QuedaRepository quedaRepository, GlpiService glpiService, QuedaUtils quedaUtils) {
        this.nagiosAPI = nagiosAPI;
        this.cidadeService = cidadeService;
        this.quedaRepository = quedaRepository;
        this.glpiService = glpiService;
        this.quedaUtils = quedaUtils;
    }

    private static final long limiteDeReboot = 660;
    private static final long JanelaLoop = 60;
    private static final long JanelaEnergia = 60;
    private static final int minQuedasParaFlap = 3;
    //Pega todas as quedas, separa por cidade, ordena por data,
    // monta “episodios” de quedas proximas e quando fecha um episódio
    // chama fecharEpisodio() para decidir se aquilo vira flap ou não.
    @Transactional
        public void identificaitorDeFlaps() {
            List<Queda> todas = quedaRepository.findAll();
            //mapeamos as quedas por cidade
            Map<String, List<Queda>> porCidade = todas.stream()
                    .filter(q -> q.getNomeCidade() != null && q.getData() != null)
                    .collect(Collectors.groupingBy(Queda::getNomeCidade));

            List<Queda> paraAtualizar = new ArrayList<>();

            //identificaitor de grupo de quedas, me ajuda a puxar as quedas no banco para debug, retirar em produção se precisar.
            long grupoSeq = 0;

            //iniciamos varredura da lista por cidade, sort pela data e verificamos as quedas atual.
            for (List<Queda> quedasCidade : porCidade.values()) {

                quedasCidade.sort(Comparator.comparing(Queda::getData));
                List<Queda> episodio = new ArrayList<>();
                for (Queda atual : quedasCidade) {
                    if (episodio.isEmpty()) { episodio.add(atual); continue; }

                    //tive que apagar as quedas do banco então esse if é para debug e recolar o check de energia.
                    if(atual.getUptime() <690){
                        atual.setFaltaDeLuz(true);
                    }


                    Queda anterior = episodio.get(episodio.size() - 1);
                    //verificamos se correspondem a um episodio de quedas em sequencia
                    if (mesmoEpisodio(anterior, atual)) {
                        episodio.add(atual);
                    } else {
                        grupoSeq = fecharEpisodio(episodio, paraAtualizar, grupoSeq);
                        episodio = new ArrayList<>();
                        episodio.add(atual);
                    }
                }
                grupoSeq = fecharEpisodio(episodio, paraAtualizar, grupoSeq);
            }

            if (!paraAtualizar.isEmpty()) quedaRepository.saveAll(paraAtualizar);
            System.out.println("Flaps marcados: " + paraAtualizar.size() + " quedas em episódios.");
        }

        //nome diz tudo
        private boolean rebootou(Queda q) {
            return q.getUptime() > 0 && q.getUptime() <= limiteDeReboot;
        }

        //comparamos os gaps entre as queds aqui, com base nas variaveis
        private boolean mesmoEpisodio(Queda anterior, Queda atual) {
            long gap = ChronoUnit.MINUTES.between(anterior.getDataUp(), atual.getData());
            if (gap < 0) gap = 0;
            if (rebootou(atual)) {
                return gap <= JanelaEnergia;
            }
            return gap <= JanelaLoop;
        }

        private long fecharEpisodio(List<Queda> episodio, List<Queda> paraAtualizar, long grupoSeq) {
            if (episodio.size() < minQuedasParaFlap ) {
                return grupoSeq;
            }
            long reboots = episodio.stream().filter(this::rebootou).count();
            boolean maioriaRebootou = reboots * 2 >= episodio.size();
            CategoriaQueda categoria = maioriaRebootou ? CategoriaQueda.FLAP_ENERGIA : CategoriaQueda.FLAP_LOOP;
            long grupo = ++grupoSeq;
            for (Queda q : episodio) {
                q.setFlap(true);
                q.setCategoria(categoria);
                q.setFlapGrupoId(grupo);
                paraAtualizar.add(q);
            }

//            Isso aqui seria para apagar os flaps e deixar como uma queda unica. como decidimos não mexer no banco n serve mais.
//            Queda principal = episodio.get(0);
//            Duration total = principal.getTempoFora() != null ? principal.getTempoFora() : Duration.ZERO;
//            for (int i = 1; i < episodio.size(); i++) {
//                Queda q = episodio.get(i);
//                if (q.getTempoFora() != null) total = total.plus(q.getTempoFora());
//                quedaRepository.delete(q); // remover as repetidas
//            }
//            principal.setTempoFora(total);
            return grupoSeq;
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

    public List<Queda> findQuedasDoDiaAtual(LocalDate data){
        List<Queda> todasQuedas = findQuedasNoBanco();
        List<Queda> atuais = quedaUtils.listaQuedasEmAndamento(todasQuedas);

        List<Queda> quedasDoDia = new ArrayList<>(quedaUtils.filterQuedasPorDia(todasQuedas, data));

        for(Queda a :  atuais){
            if(!quedasDoDia.contains(a)){
                quedasDoDia.addFirst(a);
            }else{
                quedasDoDia.remove(a); //quedas realmente do dia ficam em primeiro
                quedasDoDia.addFirst(a);
            }
        }

        return quedasDoDia;
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

                    resolveQueda(quedaBanco, quedaRecente);
                    if(!glpiBloqueado) {
                        abreGLPI(quedaBanco, quedaRecente);
                    }
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
            if (tempoDaQueda.toSeconds() > 600 && quedaUtils.horarioDeAbrirGlpi()) {
                glpiService.abrirChamado(quedaBanco.getId());
            }
        }
    }

    private void resolveQueda(Queda quedaBanco, Queda quedaRecente) {
        if (quedaBanco.getTempoFora() == Duration.ZERO && quedaRecente.getTempoFora() != Duration.ZERO) {
            quedaBanco.setTempoFora(quedaRecente.getTempoFora());
            quedaBanco.setUptime(quedaRecente.getUptime());
            log.info("Resolvendo queda com TempoFora e Uptime: {}", quedaBanco);
            quedaRepository.saveAndFlush(quedaBanco);
            if (!quedaBanco.getChamado().isBlank() && !glpiBloqueado) {
                glpiService.fecharChamado(quedaBanco.getId(), "");
            }
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
