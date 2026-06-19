package com.testeBanda.testador.service;

import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.models.Dispositivos;
import com.testeBanda.testador.repository.CidadesRepository;
import com.testeBanda.testador.repository.DispositivosRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DispositivosService {

        @Autowired
        CidadesRepository cidadesRepository;
        @Autowired
        DispositivosRepository dispositivosRepository;

        @Transactional
        public void salvarResultadosCidade(Cidades cidade, List<Dispositivos> encontrados, LocalDate hoje) {
            Cidades cidadeAnexada = cidadesRepository
                    .findByIdComDispositivos(cidade.getNome())
                    .orElse(cidade);

            Map<String, Dispositivos> existentesPorIp = cidadeAnexada.getDispositivos().stream()
                    .collect(Collectors.toMap(
                            Dispositivos::getIp,
                            d -> d,
                            (d1, d2) -> d1
                    ));

            for (Dispositivos encontrado : encontrados) {
                Dispositivos existente = existentesPorIp.get(encontrado.getIp());
                if (existente != null) {
                    existente.setNome(encontrado.getNome());
                    existente.setDescricao(encontrado.getDescricao());
                    existente.setUsuario(encontrado.getUsuario());
                    existente.setUltimaVarredura(hoje);
                    if (!existente.getCidade().equals(cidade)) existente.setCidade(cidade);
                    log.info("Atualizado: {}", existente.getIp());
                } else {
                    encontrado.setCidade(cidadeAnexada);
                    encontrado.setDataDaVarredura(hoje);
                    cidadeAnexada.getDispositivos().add(encontrado);
                    log.info("Adicionado: {}", encontrado.getIp());
                }

            }
            dispositivosRepository.saveAll(encontrados);
            cidadeAnexada.ultimaVarredura = hoje;
            cidadesRepository.save(cidadeAnexada);
        }

    @Transactional
    public void salvarResultadosOrfaos(List<Dispositivos> encontrados, LocalDate hoje) {
        Map<String, Dispositivos> unicosEncontrados = encontrados.stream()
                .collect(Collectors.toMap(Dispositivos::getIp, d -> d, (d1, d2) -> d1));
        List<String> ipsParaBuscar = unicosEncontrados.keySet().stream().toList();
        List<Dispositivos> existentes = dispositivosRepository.findByIpIn(ipsParaBuscar);
        Map<String, Dispositivos> existentesPorIp = existentes.stream()
                .collect(Collectors.toMap(Dispositivos::getIp, d -> d));
        List<Dispositivos> dispositivosParaSalvar = new ArrayList<>();
        for (Dispositivos encontrado : unicosEncontrados.values()) {
            Dispositivos existente = existentesPorIp.get(encontrado.getIp());
            if (existente != null) {
                existente.setNome(encontrado.getNome());
                existente.setCidade(null);
                existente.setUltimaVarredura(hoje);
                dispositivosParaSalvar.add(existente);
                log.info("Atualizado (órfão): {}", existente.getIp());
            } else {
                encontrado.setCidade(null);
                encontrado.setDataDaVarredura(hoje);
                dispositivosParaSalvar.add(encontrado);
                log.info("Adicionado (órfão): {}", encontrado.getIp());
            }
        }
        dispositivosRepository.saveAll(dispositivosParaSalvar);
    }



}
