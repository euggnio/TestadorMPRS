package com.testeBanda.testador.service;

import com.testeBanda.testador.api.GlpiAPI;
import com.testeBanda.testador.models.Queda;
import com.testeBanda.testador.repository.QuedaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@Service
public class GlpiService {

    private QuedaRepository quedaRepository;
    private GlpiAPI glpiAPI;

    public void editarProtocolo(long id, String protocolo) {
        Queda queda = quedaRepository.findById(id).get();
        queda.setProtocolo(protocolo);
        quedaRepository.save(queda);
        if (!queda.getChamado().isBlank()){
            glpiAPI.insertFollowUpTicket(queda.getChamado(), "Protocolo ávato : " + protocolo);
        }
    }

    public void adicionarFollowUp(long id, String texto) {
        Queda queda = quedaRepository.findById(id).get();
        log.info("Adicionando FollowUp do usuário à queda {}: '{}'", queda, texto);
        if(queda.getChamado().isBlank()){
            log.error("Erro ao adicionar FollowUp - Queda sem chamado");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Erro ao adicionar FollowUp - Queda sem chamado");
        }
        glpiAPI.insertFollowUpTicket(queda.getChamado(), "From testador: " + texto);
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
        String fechamento = """
                    <p>Chamado fechado pelo testador<p>
                    <p>Protocolo $s <p>
                    <p>Chamado $s <p>
                    <p>Tempo fora $s <p>
                    <p>Queda de energia? $s <p>
                """;
        return String.format(
                fechamento,
                queda.getProtocolo(),
                queda.getChamado(),
                queda.getTempoFora(),
                (queda.isFaltaDeLuz()? "Sim" : "nao"));
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


