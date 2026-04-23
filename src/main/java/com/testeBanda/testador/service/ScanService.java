package com.testeBanda.testador.service;

import com.testeBanda.testador.api.SnmpWanMonitor;
import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.repository.CidadesRepository;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ScanService {

    @Autowired
    CidadesRepository cidadesRepository;
    @Autowired
    SnmpWanMonitor snmpWanMonitor;

    public void adicionarDispositivos() throws IOException {
        TransportMapping transport = new DefaultUdpTransportMapping();
        Snmp snmp = new Snmp(transport);
        transport.listen();

        List<Cidades> cidades = cidadesRepository.findAll();

        for (Cidades cidade : cidades) {
            cidade.getDispositivos().clear();
            snmpWanMonitor.scanCidades(cidade,snmp);
        }

        cidadesRepository.saveAll(cidades);
        snmp.close();
    }

    public void adicionarDispositivos(String cidadeId) throws IOException {
        TransportMapping transport = new DefaultUdpTransportMapping();
        Snmp snmp = new Snmp(transport);
        transport.listen();

        Cidades cidade = cidadesRepository.findById(cidadeId)
                .orElseThrow(() -> new RuntimeException("Cidade não encontrada"));
        cidade.getDispositivos().clear();
        snmpWanMonitor.scanCidades(cidade,snmp);

        cidadesRepository.save(cidade);
        System.out.println("Cidade adicionado com sucesso!");
        snmp.close();
    }

}
