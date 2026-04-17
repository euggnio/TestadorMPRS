package com.testeBanda.testador.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.models.ResultadosSnmp;
import com.testeBanda.testador.service.CidadeService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SnmpWanMonitor {

    private final HttpClient httpClient;
    private static final String COMMUNITY = "public";

    @Value("${snmp.timeout}")
    private int TIMEOUT;
    @Value("${snmp.recheckWanCicles}")
    private int recheckWanIndexCacheCicles;
    private int recheckCounter;

    @Value("${testador.notDoSnmp}")
    private boolean notDoSnmp;

    @Value("${testador.endpointloss}")
    private String endpointloss;

    private static final OID OID_IF_NAME = new OID("1.3.6.1.2.1.31.1.1.1.1");
    private static final OID OID_IN = new OID("1.3.6.1.2.1.31.1.1.1.6");
    private static final OID OID_OUT = new OID("1.3.6.1.2.1.31.1.1.1.10");

    private Snmp snmp;

    // cache ip -> ifIndex WAN
    //Utilizamos o cache para n ficar identificando a interface, diminuindo o numero de requisiçoes
    private final Map<String,Integer> wanIndexCache = new ConcurrentHashMap<>();

    // cache ultima leitura
    private final Map<String,Long> lastRx = new ConcurrentHashMap<>();
    private final Map<String,Long> lastTx = new ConcurrentHashMap<>();
    private final Map<String,Long> lastTime = new ConcurrentHashMap<>();

    //Incrivel contador para a média de tráfego.
    @Value("${snmp.ciclos}")
    private int maxCiclos;

    public List<ResultadosSnmp> resultados = new ArrayList<>();
    @Autowired
    public SnmpWanMonitor(CidadeService cidadeService, HttpClient httpClient) {
        this.httpClient = httpClient;
        List<Cidades> cidades = cidadeService.findAll();
        for (Cidades cidade : cidades) {
            resultados.add(new ResultadosSnmp(cidade.getIp(), cidade.getVelocidadeInteger(),cidade.getSmokeID()));
        }
        this.recheckCounter = this.recheckWanIndexCacheCicles;
        this.init();
    }

    public void init(){
        if(notDoSnmp){
            return;
        }
        TransportMapping<UdpAddress> transport = null;
        try {
            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();
        } catch (IOException e) {
            log.error("ERRO ao iniciar o SnmpMonitor : " + e.getMessage());
        }
    }

    //aqui geramos a requisição snmp para o ip x.
    private CommunityTarget<Address> createTarget(String ip){
        CommunityTarget<Address> target = new CommunityTarget<>();
        target.setCommunity(new OctetString(COMMUNITY));
        target.setAddress(GenericAddress.parse("udp:"+ip+"/161"));
        target.setTimeout(TIMEOUT);
        target.setRetries(3);
        target.setVersion(SnmpConstants.version2c);

        return target;
    }

    // ------------------------
    // DESCOBRIR INTERFACE WAN
    // ------------------------

    private Integer discoverWan(String ip) throws Exception {

        System.out.println("\nSNMP WALK em "+ip);

        long start = System.currentTimeMillis();

        OID root = OID_IF_NAME;

        PDU pdu = new PDU();
        pdu.add(new VariableBinding(root));
        pdu.setType(PDU.GETBULK);
        pdu.setMaxRepetitions(20);
        pdu.setNonRepeaters(0);
        //criamos a requisição para a rb x que precisamos descobrir a interface wan
        CommunityTarget<Address> target = createTarget(ip);

        while(true){

            ResponseEvent<Address> event = snmp.send(pdu,target);

            if(event.getResponse()==null) {
                System.out.println("Timeout WALK "+ip);
                return null;
            }

            PDU response = event.getResponse();

            VariableBinding vb = response.get(0);

            if(vb.getOid()==null || !vb.getOid().startsWith(root))
                break;

            String name = vb.getVariable().toString().toLowerCase();

            int index = vb.getOid().last();

            System.out.println("Interface "+index+" -> "+name);

            if(name.contains("wan")){

                long time = System.currentTimeMillis()-start;
                System.out.println("WAN encontrada index "+index+" ("+time+" ms)");
                return index;

            }
            pdu.setRequestID(new Integer32(0));
            pdu.set(0,new VariableBinding(vb.getOid()));
        }

        return null;

    }

    // ------------------------
    // GET RX TX
    // ------------------------

    private void getTraffic(ResultadosSnmp data, int index) throws Exception {
        OID in = new OID(OID_IN).append(index);
        OID out = new OID(OID_OUT).append(index);

        PDU pdu = new PDU();
        pdu.add(new VariableBinding(in));
        pdu.add(new VariableBinding(out));

        pdu.setType(PDU.GET);

        ResponseEvent<Address> event = snmp.send(pdu,createTarget(data.getIp()));

        if(event.getResponse()==null){

            System.out.println("Router "+data.getIp()+" TIMEOUT");
            return;

        }

        PDU response = event.getResponse();

        long rx = response.get(0).getVariable().toLong();
        long tx = response.get(1).getVariable().toLong();

        if(lastRx.containsKey(data.getIp())){

            long deltaRx = rx-lastRx.get(data.getIp());
            long deltaTx = tx-lastTx.get(data.getIp());

            long deltaTime = System.currentTimeMillis()-lastTime.get(data.getIp());

            if(deltaRx < 0 || deltaTx < 0){
                return;
            }

            double rxMbps = (deltaRx*8.0)/(deltaTime*1000);
            double txMbps = (deltaTx*8.0)/(deltaTime*1000);

            if ( data.getDados().size() > maxCiclos ){
                data.removerDados();
            }
                data.somarDados();
                data.adicionaDados(new double[]{rxMbps,txMbps});

            }

        lastRx.put(data.getIp(), rx);
        lastTx.put(data.getIp(),tx);
        lastTime.put(data.getIp(),System.currentTimeMillis());

    }

    // ------------------------
    // LOOP
    // ------------------------
    public void run(){
        if(notDoSnmp){
            return;
        }
        recheckCounter--;
        //verificar se o cache está vazio
        if(wanIndexCache.isEmpty() || recheckCounter <= 0){
            System.out.println("Descobrindo interfaces WAN...\n");
            for(ResultadosSnmp ip:resultados){
                Integer index = null;
                try {
                    index = discoverWan(ip.getIp());
                } catch (Exception e) {
                    log.debug("ERRO ao gerar cache de WAN : " + e.getMessage());
                }
                wanIndexCache.put(ip.getIp(), Objects.requireNonNullElse(index, -1));
            }

            System.out.println("\nCache WAN:");
            System.out.println(wanIndexCache);
            System.out.println("Resetando recheck WAN IndexCache");
            recheckCounter = recheckWanIndexCacheCicles;
        }

            resultados.parallelStream().forEach((resultado)->{
                try {
                    //não pega trafego de roteadores que não foi possivel pegar o wanID
                    if ( wanIndexCache.get(resultado.getIp()) == -1 ){
                        System.out.println("Roteador fora do ar, IP: "+resultado.getIp());
                        return;
                    }
                    getTraffic(resultado, wanIndexCache.get(resultado.getIp()));
                } catch (Exception e) {
                    log.error("ERRO ao pegar trafego de IP {}: {}", resultado.getIp(), e.getMessage());
                }
            });
    }

    @PreDestroy
    public void close() throws IOException {
        snmp.close();
    }

    private String lastModifiedDate = null;
    public  void setHostLoss(){
        URI url = URI.create(endpointloss);
        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(url)
                .header("If-Modified-Since", lastModifiedDate != null ? lastModifiedDate : "")
                .GET()
                .build();
        HttpResponse<String> response = null;
        Map<String,Double> hosts;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 304) {
                return;
            }
            response.headers().firstValue("Last-Modified").ifPresent(date -> lastModifiedDate = date);
            JsonNode json = new ObjectMapper().readTree(response.body());
            JsonNode node = json.get("hosts");
            ObjectMapper mapper = new ObjectMapper();
            hosts = mapper.convertValue(node, new TypeReference<Map<String,Double>>(){});
        } catch (IOException | InterruptedException e) {
            log.error("ERRO ao pegar dados de LOSS: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        resultados.forEach(resultado->{
            if(hosts != null){
                double loss = hosts.get(resultado.getSmokeID());
                resultado.adicionarLoss(loss);
            }
            resultado.adicionarLoss(0);

        });
    }


    public static void main(String[] args) throws IOException, InterruptedException {



    }
}