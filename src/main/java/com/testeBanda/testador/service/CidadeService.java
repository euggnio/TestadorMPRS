package com.testeBanda.testador.service;

import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.models.Contato;
import com.testeBanda.testador.repository.CidadesRepository;
import com.testeBanda.testador.repository.ContatoRepository;
import com.testeBanda.testador.repository.ResultadosRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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
        cidade.setVelocidade(cidade.getVelocidade() +" Mbps");
        cidadesRepository.save(cidade);
    }

    public void apagarCidade(String id) {
        cidadesRepository.deleteById(id);
    }

}