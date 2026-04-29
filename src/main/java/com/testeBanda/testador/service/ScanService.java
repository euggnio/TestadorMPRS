package com.testeBanda.testador.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.testeBanda.testador.api.GlpiAPI;
import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.models.Dispositivos;
import com.testeBanda.testador.repository.CidadesRepository;
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

    public void varrerCidades() {
        List<Cidades> cidades = cidadesRepository.findAll();
        if (cidades.isEmpty()) return;
        LocalDate hoje = LocalDate.now(); //## cache da data para não chamar várias vezes

        try (TransportMapping transport = new DefaultUdpTransportMapping();
             Snmp snmp = new Snmp(transport); ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();) {

            transport.listen();
            glpiAPI.getSessionToken();

            for (Cidades cidade : cidades) {
                if (cidade.ultimaVarredura == null) {
                    cidade.ultimaVarredura = hoje.minusDays(1);
                }
                if (cidade.ultimaVarredura.equals(hoje)) {
                    log.info("Varredura já efetuada em {}", cidade.getNome());
                    continue;
                }

                log.info(">>>> Iniciando varredura em: {}", cidade);
                List<Dispositivos> encontrados = scanearHost(snmp, cidade, executor);

                //CRIAMOS UM MAP PARA FACILITAR A REPETIÇÃO SENÃO 255 * 180 * 255  ( IPS DISPOSITIVOS * CIDADES * DISPOSITIVOS JÁ SALVOS )
                Map<String, Dispositivos> existentesPorIp = cidade.getDispositivos().stream()
                        .collect(Collectors.toMap(Dispositivos::getIp, d -> d));
                //TODO APAGAR OS COMENTARIOS DEPOIS O QUE ARTUR LER
                //Verificamos se o dispositivo existe na PJ E ATUALIZAMOS
                //ADICIONAMOS NOVO CASO ELE N EXISTA
                // DISPOSITIVOS QUE FICARAM OFF VAO ESTAR COM DATA ATRASADA DE VERIFICAÇÃO AI VERIFICAMOS NO FRONT
                for (Dispositivos encontrado : encontrados) {
                    Dispositivos existente = existentesPorIp.get(encontrado.getIp());

                    if (existente != null) {
                        existente.setNome(encontrado.getNome());
                        existente.setDescricao(encontrado.getDescricao());
                        existente.setUsuario(encontrado.getUsuario());
                        existente.setUltimaVarredura(hoje);
                        log.info("Atualizado: {}", existente.getIp());
                    } else {
                        encontrado.setCidade(cidade);
                        encontrado.setDataDaVarredura(hoje);
                        cidade.getDispositivos().add(encontrado);
                        log.info("Adicionado: {}", encontrado.getIp());
                    }
                }

                cidade.ultimaVarredura = hoje;
                cidadesRepository.save(cidade);
                log.info("Varredura salva para cidade: {}", cidade.getNome());
                Thread.sleep(1000);
            }
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private List<Dispositivos> scanearHost(Snmp snmp, Cidades cidade, ExecutorService executor) throws IOException {
        Semaphore semaphore = new Semaphore(20);
        String[] partesIp = cidade.getIp().split("\\.");
        int terceiroOctetoBase = Integer.parseInt(partesIp[2]);
        String prefixoRede = partesIp[0] + "." + partesIp[1] + ".";
        List<Future<?>> futures = new ArrayList<>();
        List<Dispositivos> dispositivos = Collections.synchronizedList(new ArrayList<>());

        Map<String, String> arpCache = carregarArpDoMikrotik(snmp, prefixoRede + terceiroOctetoBase + ".1", "public");
        int loops = 1;
        if(cidade.getNotacao() != null){
            loops = cidade.getNotacao().equals("23") ? 2 : 1;
        }

        for (int i = 0; i < loops; i++) {
            int terceiroOctetoAtual = terceiroOctetoBase + i;
            String baseIpAtual = prefixoRede + terceiroOctetoAtual;
            for (int fim = 2; fim <= 254; fim++) {
                final String ip = baseIpAtual + "." + fim;

                Future<?> future = executor.submit(() -> {
                    try {
                        semaphore.acquire();
                        try {
                            Dispositivos d = processarHost(snmp, ip, arpCache);
                            if (d != null) {
                                dispositivos.add(d);
                                System.out.println("Encontrado: " + d);
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
            System.out.println(ip + " OFF");
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
            target.setTimeout(2000);
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
    //METODO POR RCP
    //    public static String ultimoUsuario(String host) {
//        try {
//            ProcessBuilder pb = new ProcessBuilder(
//                    "cmd", "/c",
//                    "reg query \\\\" + host +
//                            "\\HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Authentication\\LogonUI /v LastLoggedOnUser"
//            );
//
//            pb.redirectErrorStream(true);
//            Process p = pb.start();
//            // timeout
//            boolean terminou = p.waitFor(10, TimeUnit.SECONDS);
//            if (!terminou) {
//                p.destroyForcibly();
//                return "N/A";
//            }
//            BufferedReader br = new BufferedReader(
//                    new InputStreamReader(p.getInputStream())
//            );
//            String line;
//            while ((line = br.readLine()) != null) {
//
//                if (line.contains("LastLoggedOnUser")) {
//
//                    String[] parts = line.trim().split("\\s+");
//
//                    if (parts.length >= 3) {
//                        String usuario = parts[parts.length - 1];
//
//                        // remove dominio\
//                        if (usuario.contains("\\")) {
//                            usuario = usuario.substring(
//                                    usuario.indexOf("\\") + 1
//                            );
//                        }
//
//                        return usuario;
//                    }
//                }
//            }
//        } catch (Exception e) {
//            return "Erro";
//        }
//        return "Sem usuário";
//    }

}
