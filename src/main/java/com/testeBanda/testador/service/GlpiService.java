package com.testeBanda.testador.service;

import com.testeBanda.testador.api.GlpiAPI;
import com.testeBanda.testador.models.Queda;
import com.testeBanda.testador.repository.QuedaRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Optional;

@Slf4j
@Service
public class GlpiService {

    private final QuedaRepository quedaRepository;
    private final GlpiAPI glpiAPI;

    public GlpiService(QuedaRepository quedaRepository, GlpiAPI glpiAPI) {
        this.quedaRepository = quedaRepository;
        this.glpiAPI = glpiAPI;
    }

    @Transactional
    public void editarProtocolo(long id, String protocolo) {
        Queda queda = quedaRepository.findById(id).get();
        queda.setProtocolo(protocolo);
        quedaRepository.save(queda);
        if (queda.getChamado().isBlank()){
            this.abrirChamado(id);
        }
        glpiAPI.insertFollowUpTicket(queda.getChamado(), "<p>Protocolo da ávato : " + protocolo + "</p>");
    }

    public void adicionarFollowUp(long id, String texto) {
        Queda queda = quedaRepository.findById(id).get();
        log.info("Adicionando FollowUp do usuário à queda {}: '{}'", queda, texto);
        if(queda.getChamado().isBlank()){
            log.error("Erro ao adicionar FollowUp - Queda sem chamado");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Erro ao adicionar FollowUp - Queda sem chamado");
        }
        glpiAPI.insertFollowUpTicket(queda.getChamado(), "<p>Testador: " + texto + "</p>");
    }

    public ArrayList<String> getFollowUpTicket(long id) {
        Optional<Queda> queda = quedaRepository.findById(id);
        if(queda.isEmpty()){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Falha");
        }
        log.info("Get FollowUpTicket referente a queda: {}", queda);

        return glpiAPI.getTicketFollowups(queda.get().getChamado());
    }

    public void fecharChamado(long id, String texto) {
        Queda queda = quedaRepository.findById(id).get();
        log.info("Fechando chamado referente a queda: {}", queda);
        if(queda.getChamado().isBlank()){
            log.info("Erro ao fechar chamado - Queda não foi encontrada");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Falha no fechamento do chamado - Não há chamado");
        }
        String fechamentoFormatado = getStringDeFechamento(queda);

        glpiAPI.insertFollowUpTicket(queda.getChamado(), fechamentoFormatado);
        if(!texto.isBlank()){
            glpiAPI.insertFollowUpTicket(queda.getChamado(), texto);
        }
        glpiAPI.closeGlpiTicket(queda.getChamado());
    }

    private static String getStringDeFechamento(Queda queda) {
        return "<div>" +
                "<p>Chamado fechado pelo testador</p>" +
                "<p>Protocolo da operadora: "+ queda.getProtocolo()+ " </p>" +
                "<p>Chamado do GLPI: "+ queda.getChamado()+" </p>" +
                "<p>Tempo fora: "+ queda.getTempoFora().toMinutes()+" minutos. </p>" +
                "<p>Tempo de uptime: "+ queda.getUptime()+" segundos. </p>" +
                "<p>Foi queda de energia? "+ (queda.isFaltaDeLuz() ? "Sim": "Não")+" </p> " +
                "</div>";
    }

    public void abrirChamado(long id) {
        Optional<Queda> queda = quedaRepository.findById(id);
        if(queda.isEmpty()){
            log.info("Erro ao abrir chamado - Queda não foi encontrada");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Falha");
        }
        String ticket = glpiAPI.createGlpiTicket(queda.get().getCidade().getNome());
        queda.get().setChamado(ticket);
        log.info("Abrir chamado {} referente a queda: {}", ticket, queda);
        quedaRepository.save(queda.get());
    }

    public void atribuirResponsavel(long id, String user) {
        Optional<Queda> queda = quedaRepository.findById(id);
        if(queda.isEmpty()){
            log.info("Erro ao atribuir chamado - Queda não foi encontrada");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Falha");
        }
        log.info("Atribuir Responsavel referente a queda: {}", queda);
        glpiAPI.assignTicketToUser(queda.get().getChamado(), user);
        queda.get().setResponsavel(user);
        quedaRepository.save(queda.get());
    }
}


