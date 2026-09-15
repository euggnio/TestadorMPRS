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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DispositivosService {

        @Autowired
        CidadesRepository cidadesRepository;
        @Autowired
        DispositivosRepository dispositivosRepository;

        private final ConcurrentHashMap<String, Object> cityLocks = new ConcurrentHashMap<>();

        @Transactional
        public void salvarResultadosCidade(Cidades cidade, List<Dispositivos> encontrados, LocalDate hoje) {
            Object lock = cityLocks.computeIfAbsent(cidade.getNome(), k -> new Object());
            synchronized (lock) {
                try {
                    String threadName = Thread.currentThread().getName();
                    log.info("[{}] INICIO salvarResultadosCidade para {} ({} dispositivos encontrados)",
                            threadName, cidade.getNome(), encontrados.size());

                    List<String> ipsEncontrados = encontrados.stream()
                            .map(Dispositivos::getIp)
                            .toList();

                    List<Dispositivos> existentes = dispositivosRepository.findByIpIn(ipsEncontrados);
                    Map<String, Dispositivos> existentesPorIp = existentes.stream()
                            .collect(Collectors.toMap(
                                    Dispositivos::getIp,
                                    d -> d,
                                    (d1, d2) -> d1
                            ));

                    List<Dispositivos> dispositivosParaSalvar = new ArrayList<>();
                    int atualizados = 0;
                    int adicionados = 0;

                    for (Dispositivos encontrado : encontrados) {
                        Dispositivos existente = existentesPorIp.get(encontrado.getIp());

                        if (existente != null) {
                            existente.setNome(encontrado.getNome());
                            existente.setDescricao(encontrado.getDescricao());
                            existente.setUsuario(encontrado.getUsuario());
                            existente.setUltimaVarredura(hoje);
                            if (existente.getCidade() == null || !existente.getCidade().getNome().equals(cidade.getNome())) {
                                existente.setCidade(cidade);
                            }
                            dispositivosParaSalvar.add(existente);
                            atualizados++;
                            log.info("[{}] Atualizado: {} (cidade: {})", threadName, existente.getIp(), cidade.getNome());
                        } else {
                            encontrado.setCidade(cidade);
                            encontrado.setDataDaVarredura(hoje);
                            dispositivosParaSalvar.add(encontrado);
                            adicionados++;
                            log.info("[{}] Adicionado: {} (cidade: {})", threadName, encontrado.getIp(), cidade.getNome());
                        }
                    }

                    if (!dispositivosParaSalvar.isEmpty()) {
                        dispositivosRepository.saveAll(dispositivosParaSalvar);
                    }

                    Cidades cidadeDb = cidadesRepository.findById(cidade.getNome()).orElse(cidade);
                    cidadeDb.setUltimaVarredura(LocalDateTime.now());
                    cidadesRepository.save(cidadeDb);

                    log.info("[{}] FIM salvarResultadosCidade para {} (atualizados: {}, adicionados: {})",
                            threadName, cidade.getNome(), atualizados, adicionados);
                } finally {
                    cityLocks.remove(cidade.getNome());
                }
            }
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
