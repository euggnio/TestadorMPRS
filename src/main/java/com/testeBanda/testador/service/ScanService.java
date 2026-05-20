package com.testeBanda.testador.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.testeBanda.testador.api.GlpiAPI;
import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.models.Dispositivos;
import com.testeBanda.testador.repository.CidadesRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ScanService {

    @Autowired
    CidadesRepository cidadesRepository;
    @Autowired
    GlpiAPI glpiAPI;

    @Value("${varredura.desligar}")
    private boolean desligar;

    public void varrerCidades() {
        if (desligar) return;

        List<Cidades> cidades = cidadesRepository.findAll();
        if (cidades.isEmpty()) return;

        LocalDate hoje = LocalDate.now();
        Semaphore semaforoScans = new Semaphore(80);
        ExecutorService routerExecutor = Executors.newFixedThreadPool(2);
        ExecutorService vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        Semaphore semaforoPjs = new Semaphore(2);

        try (TransportMapping transport = new DefaultUdpTransportMapping();
             Snmp snmp = new Snmp(transport)) {

            transport.listen();
            glpiAPI.getSessionToken();

            for (Cidades cidade : cidades) {
                if (hoje.equals(cidade.getUltimaVarredura())) {
                    log.info("Varredura já efetuada em {}", cidade.getNome());
                    continue;
                }
                routerExecutor.submit(() -> {
                    try {
                        semaforoPjs.acquire();
                        log.info(">>>> Iniciando varredura em: {}", cidade.getNome());
                        List<Dispositivos> encontrados = scanearHost(snmp, cidade, vThreadExecutor,semaforoScans);
                        salvarResultados(cidade, encontrados, hoje);
                        log.info("Varredura salva para cidade: {}", cidade.getNome());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Thread interrompida para {}", cidade.getNome());
                    } catch (Exception e) {
                        log.error("Erro na varredura de {}: {}", cidade.getNome(), e.getMessage());
                    } finally {
                        semaforoPjs.release();
                    }
                });
            }

            routerExecutor.shutdown();
            if (!routerExecutor.awaitTermination(2, TimeUnit.HOURS)) {
                log.warn("routerExecutor não terminou");
                routerExecutor.shutdownNow();
            }
            vThreadExecutor.shutdown();
            if (!vThreadExecutor.awaitTermination(10, TimeUnit.MINUTES)) {
                log.warn("vThreadExecutor não terminou");
                vThreadExecutor.shutdownNow();
            }
        } catch (IOException e) {
            log.error("Erro na camada de rede: {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Varredura interrompida: {}", e.getMessage());
            routerExecutor.shutdownNow();
            vThreadExecutor.shutdownNow();
        }
    }


    private List<Dispositivos> scanearHost(Snmp snmp, Cidades cidade, ExecutorService executor, Semaphore semaphore) {
        String[] partesIp = cidade.getIp().split("\\.");
        int terceiroOctetoBase = Integer.parseInt(partesIp[2]);
        String prefixoRede = partesIp[0] + "." + partesIp[1] + ".";
        List<Future<?>> futures = new ArrayList<>();
        List<Dispositivos> dispositivos = Collections.synchronizedList(new ArrayList<>());

        Map<String, String> arpCache = carregarArpDoMikrotik(snmp, prefixoRede + terceiroOctetoBase + ".1", "public");
        int notacao = Integer.parseInt(cidade.getNotacao());
        int loops = 1 << (24 - notacao);
        int inicioDosIps = Integer.parseInt(partesIp[3]);
        int limiteDosIps = 254;
        if(inicioDosIps == 1 && cidade.getNotacao().equals("25")) {
            limiteDosIps = 128;
        }
        for (int i = 0; i < loops; i++) {
            int terceiroOctetoAtual = terceiroOctetoBase + i;
            String baseIpAtual = prefixoRede + terceiroOctetoAtual;
            for (int atual = inicioDosIps ; atual <= limiteDosIps; atual++) {
                final String ip = baseIpAtual + "." + atual;

                Future<?> future = executor.submit(() -> {
                    try {
                        semaphore.acquire();
                        try {
                            Dispositivos d = processarHost(snmp, ip, arpCache);
                            if (d != null) {
                                dispositivos.add(d);
                            }
                        } finally {
                            semaphore.release();
                        }
                    } catch (Exception e) {
                        dispositivos.add(new Dispositivos(ip, "ERRO" , e.getMessage(),null));
                    }
                    return null;
                });
                futures.add(future);
            }
        }
        for (Future<?> future : futures) {
            try {
                future.get(5, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                future.cancel(true);
            } catch (Exception e) {
                System.err.println("Erro na task: " + e.getMessage());
            }
        }
        return dispositivos;
    }

    @Transactional
    public void salvarResultados(Cidades cidade, List<Dispositivos> encontrados, LocalDate hoje) {
        Cidades cidadeAnexada = cidadesRepository
                .findByIdComDispositivos(cidade.getNome())
                .orElse(cidade);
        Map<String, Dispositivos> existentesPorIp = cidadeAnexada.getDispositivos().stream()
                .collect(Collectors.toMap(Dispositivos::getIp, d -> d));

        for (Dispositivos encontrado : encontrados) {
            Dispositivos existente = existentesPorIp.get(encontrado.getIp());

            if (existente != null) {
                existente.setNome(encontrado.getNome());
                existente.setDescricao(encontrado.getDescricao());
                existente.setUsuario(encontrado.getUsuario());
                existente.setUltimaVarredura(hoje);
                if(!existente.getCidade().equals(cidade)) {
                    existente.setCidade(cidade);
                }
                log.info("Atualizado: {}", existente.getIp());
            } else {
                encontrado.setCidade(cidadeAnexada); // Atenção: associe a cidade anexada!
                encontrado.setDataDaVarredura(hoje);
                cidadeAnexada.getDispositivos().add(encontrado);
                log.info("Adicionado: {}", encontrado.getIp());
            }
        }

        cidadeAnexada.ultimaVarredura = hoje;

        // Agora o save vai funcionar perfeitamente, pois a entidade está sob os cuidados do Hibernate
        cidadesRepository.save(cidadeAnexada);
    }

    private Dispositivos processarHost(Snmp snmp, String ip, Map<String,String> arp) throws IOException {
        InetAddress inet = InetAddress.getByName(ip);
        String hostname = "";
        String descricao= "N/A";
        String nomeUsuario = "N/A";

        //ta pingavel?
        if (inet.isReachable(500)) {
            String name = inet.getHostName();
            //não resolvido com tombo vai snmp
            if (name.equals(ip)) {
                String[] dados = scanHost(snmp, ip, 161, "public");
                //snmp deu erro vai de arp
                if (dados.length < 2 ) {
                    hostname = arp.getOrDefault(ip, "N/A");
                    if (hostname.contains("48:7A")) {
                        hostname = "ALCATEL VOIP";
                    }
                }else{
                    hostname = dados[0];
                    descricao = dados[1];
                }
            }
            //Its a PC
            else {
                String nomeHost = name.contains(".")
                        ? name.substring(0, name.indexOf("."))
                        : name;
                JsonNode node = glpiAPI.getComputerData(nomeHost);
                hostname = nomeHost;
                nomeUsuario = Objects.equals(node.get("70").asText(), "null") ? "N/A" : node.get("70").asText();
                descricao = node.get("45").asText();
            }
        }
        else {
            return null;
        }
        return new Dispositivos(ip,hostname,descricao,nomeUsuario);
    }

    public  Map<String, String> carregarArpDoMikrotik(Snmp snmp, String mikrotikIp, String community) {
        Map<String, String> arpCache = new HashMap<>();
        try {
            Address targetAddress = new UdpAddress(mikrotikIp + "/161");
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(community));
            target.setAddress(targetAddress);
            target.setRetries(1);
            target.setTimeout(3000);
            target.setVersion(SnmpConstants.version2c);

            OID baseOid = new OID("1.3.6.1.2.1.4.22.1.2");
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(baseOid));
            pdu.setType(PDU.GETNEXT);

            while (true) {
                ResponseEvent event = snmp.send(pdu, target);
                if (event == null || event.getResponse() == null) break;

                VariableBinding vb = event.getResponse().get(0);
                if (vb == null || !vb.getOid().startsWith(baseOid)) break;

                String[] parts = vb.getOid().toString().split("\\.");
                String ip = parts[parts.length-4] + "." +
                        parts[parts.length-3] + "." +
                        parts[parts.length-2] + "." +
                        parts[parts.length-1];

                OctetString macOctet = (OctetString) vb.getVariable();
                byte[] bytes = macOctet.getValue();
                if (bytes.length == 6) {
                    StringBuilder mac = new StringBuilder();
                    for (int i = 0; i < bytes.length; i++) {
                        if (i > 0) mac.append(":");
                        mac.append(String.format("%02X", bytes[i] & 0xFF));
                    }
                    arpCache.put(ip, mac.toString());
                }

                pdu = new PDU();
                pdu.add(new VariableBinding(vb.getOid()));
                pdu.setType(PDU.GETNEXT);
            }

            System.out.println("ARP cache carregado: " + arpCache.size() + " entradas");

        } catch (Exception e) {
            System.out.println("Erro ARP SNMP Mikrotik: " + e.getMessage());
        }
        return arpCache;
    }

    private  String[] scanHost(Snmp snmp, String ip, int port, String community) {
        try {
            Address targetAddress = new UdpAddress(ip + "/" + port);

            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(community));
            target.setAddress(targetAddress);
            target.setRetries(1);
            target.setTimeout(3000);
            target.setVersion(SnmpConstants.version2c);

            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.1.1.0")));
            pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.1.5.0")));
            pdu.setType(PDU.GET);

            ResponseEvent response = snmp.send(pdu, target);

            if ( response == null || response.getResponse() == null ) return new String[0];;

            VariableBinding vb = response.getResponse().get(0);
            VariableBinding vbx = response.getResponse().get(1);

            if (vb == null || vb.getVariable() == null) {
                return new String[0];
            }
            return new String[] {
                    vbx.getVariable().toString(),
                    vb.getVariable().toString()
            };

        } catch (Exception e) {
            return new String[0];
        }
    }


}
