package com.testeBanda.testador.service;

import com.testeBanda.testador.DTO.EventoDTO;
import com.testeBanda.testador.models.Eventos;
import com.testeBanda.testador.repository.EventoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EventosService {

    @Autowired
    private EventoRepository eventoRepository;

    public void criarEventoByDTO(EventoDTO eventoDTO) {
        if (eventoDTO.dataFim().isBefore(eventoDTO.dataInicio())) {
            throw new IllegalArgumentException("A data final deve ser posterior à inicial.");
        }
        Eventos evento = new Eventos(eventoDTO.nome(),eventoDTO.dataInicio(),eventoDTO.dataFim(),eventoDTO.descricao());
        eventoRepository.save(evento);
    }


    public List<Eventos> pegarTodos() {
        return eventoRepository.findAll();
    }

    public void apagarEvento(Long id) {
        eventoRepository.deleteById(id);
    }
}
