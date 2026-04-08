package com.testeBanda.testador.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testeBanda.testador.models.Alerta;
import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.models.Disponibilidade;
import com.testeBanda.testador.models.Mes;
import com.testeBanda.testador.repository.CidadesRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.util.*;

@Service
public class NagiosAPI {

    private final CidadesRepository cidadesRepository;
    @Value("${testador.nagios}")
    private String nagios;

    public NagiosAPI(CidadesRepository cidadesRepository) {
        this.cidadesRepository = cidadesRepository;
    }

    public List<Alerta> todosAlertasDoAno(){
        String url =  nagios +"archivejson.cgi?query=alertlist&statetypes=hard&hoststates=up+down&servicestates=critical&" +
                "starttime="+getDateLimits()[0]+"&endtime="+ getDateLimits()[1];
        JsonNode alertListNode = sendRequest(url,"alertlist");
        return criaListaDeAlertas(alertListNode);
    }

    public List<Alerta> todosAlertasDesde2023(){
        Instant instant = Instant.parse("2023-01-01T00:01:00Z");
        long inicio = instant.getEpochSecond();
        long fim = Instant.now().getEpochSecond();
        String url =  nagios + "archivejson.cgi?query=alertlist&statetypes=hard&hoststates=up+down&servicestates=critical&"
                + "starttime=" + inicio + "&endtime=" + fim;
        JsonNode alertListNode = sendRequest(url,"alertlist");
        return criaListaDeAlertas(alertListNode);
    }

    private List<Alerta> criaListaDeAlertas(JsonNode json) {
        List<Alerta> alertas = new ArrayList<>();
        if (json.isArray()) {
            for (JsonNode alert : json) {
                String name = alert.path("name").asText();
                if(name.equals("1Teste")) continue;
                String tipo = alert.path("plugin_output").asText();
                long timestampMillis = alert.path("timestamp").asLong();
                LocalDateTime data = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(timestampMillis),
                        TimeZone.getDefault().toZoneId()
                );
                alertas.add(new Alerta(name, data, tipo));
            }
        }
        return alertas;
    }

    public ArrayList<Mes> relatorioDeDisponibilidade(int valorAno){
        ArrayList<Mes> meses = new ArrayList<>();
        Year ano = Year.of(valorAno);
        Month mesPresente = LocalDate.now().getYear() == valorAno ? LocalDate.now().getMonth() : Month.of(12);
        for (Month m : Month.values()) {
            LocalDateTime inicoData = ano.atMonth(m.getValue()).atDay(1).atStartOfDay();
            LocalDateTime fimData = ano.atMonth(m.getValue()).atEndOfMonth().atTime(23, 59, 59);
            ZoneId zone = ZoneId.systemDefault();
            long inicioTimestamp = inicoData.atZone(zone).toEpochSecond();
            long fimTimestamp = fimData.atZone(zone).toEpochSecond();
            String url = nagios +
                    "archivejson.cgi?query=availability&availabilityobjecttype=" +
                    "hosts&starttime=" + inicioTimestamp + "&" +
                    "endtime=" + fimTimestamp;
            JsonNode alertListNode = sendRequest(url,"hosts");

            ArrayList<Disponibilidade> cidade = new ArrayList<>();
            if ( alertListNode.isArray() ) {
                for (JsonNode alert : alertListNode) {
                    String name = alert.path("name").asText();
                    long upTime = alert.path("time_up").asLong();
                    long downTime = alert.path("time_down").asLong();
                    long total = upTime + downTime;
                    double disponibilidade = total > 0 ? (upTime * 100.0 / total) : 0.0;
                    double indisponibilidade = 100.0 - disponibilidade;
                    if(!name.equalsIgnoreCase("localhost")){
                        cidade.add(new Disponibilidade(name, disponibilidade, indisponibilidade));
                    }
                }
            }

            Mes mesAtual = new Mes(m);
            if( mesAtual.month.getValue() <= mesPresente.getValue()) {
                mesAtual.disponibilidades.addAll(cidade);
                meses.add(mesAtual);
            }
        }
        return meses;
    }

    private JsonNode sendRequest(String url, String root) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Basic " + this.getAuth())
                .build();
        JsonNode alertListNode = null;
        try{
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response.body());
            alertListNode = rootNode.path("data").path(root);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return alertListNode;
    }

    private long[] getDateLimits(){
        Year ym = Year.now();
        LocalDateTime startTime = ym.atMonth(1).atDay(1).atStartOfDay();
        LocalDateTime endTime = ym.atMonth(12).atEndOfMonth().atTime(23, 59, 59);
        ZoneId zone = ZoneId.systemDefault();
        long startTimesmap = startTime.atZone(zone).toEpochSecond();
        long endTimestamp = endTime.atZone(zone).toEpochSecond();
        long[] dateLimits = new long[2];
        dateLimits[0] = startTimesmap;
        dateLimits[1] = endTimestamp;
        return dateLimits;
    }


    //função de uso unico
    public void preencherCoordenadas(List<Cidades> cidades){
        String url = "http://nagiosmpls.mp.rs.gov.br/nagios/cgi-bin/objectjson.cgi?query=host&hostname=";
        List<JsonNode> nodes = new ArrayList<>();
        for (Cidades cidade : cidades) {
            String urlCidade = url + cidade.getNagiosID();
            JsonNode node = sendRequest(urlCidade, "host");
            String json = node.path("notes").asText().replace("<latlng>", "").replace("</latlng>", "");
            System.out.println(json);
            cidade.coordenadas = json;
            cidadesRepository.save(cidade);
        }
    }

    @Value("${nagios.password}")
    private String password;
    @Value("${nagios.username}")
    private String username;

    private String getAuth(){
        String auth = username + ":" + password;
        return Base64.getEncoder().encodeToString(auth.getBytes());
    }


}
