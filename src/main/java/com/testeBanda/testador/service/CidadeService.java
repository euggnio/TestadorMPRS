package com.testeBanda.testador.service;

import ch.qos.logback.classic.boolex.MarkerList;
import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.models.Contato;
import com.testeBanda.testador.repository.CidadesRepository;
import com.testeBanda.testador.repository.ContatoRepository;
import com.testeBanda.testador.repository.ResultadosRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
@Slf4j
@Service
public class CidadeService {

    @Autowired
    private CidadesRepository cidadesRepository;

    @Autowired
    private ContatoRepository contatoRepository;

    @Autowired
    private ResultadosRepository resultadosRepository;

    @Transactional
    public void salvarDadosHost(Cidades host) {
        Cidades cidade = cidadesRepository.findById(host.nome).orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        cidade.addResultado(now, host.ultimoTesteBanda);
        cidade.ultimoTesteBanda = host.ultimoTesteBanda;
        cidade.checkTesteBanda = host.checkTesteBanda;
        cidade.dataUltimoTeste = (now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        cidadesRepository.save(cidade);
        resultadosRepository.deleteExcedentes(cidade.getNome());
        log.info(MarkerFactory.getMarker("WARN"), "Cidade nova salva com sucesso {}", cidade.getNome());
    }

    public List<Cidades> findAll() {
        return cidadesRepository.findAll();
    }

    public Cidades findById(String id) {
        return  cidadesRepository.findById(id).get();
    }

    public List<Contato> findAllContatos() {
        return contatoRepository.findAll();
    }

    public void salvarCidade(Cidades cidade) {
        if(!cidade.velocidade.contains("Mbps")){
            cidade.setVelocidade(Integer.parseInt(cidade.getVelocidade()) +" Mbps");
        }
        log.info("Cidade foi modificada ou adicionada: " + cidade.getNome());
        cidadesRepository.save(cidade);
    }

    public void apagarCidade(String id) {
        log.info("Cidade apagada {}", id);
        cidadesRepository.deleteById(id);
    }

}