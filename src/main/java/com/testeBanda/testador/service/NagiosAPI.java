package com.testeBanda.testador.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testeBanda.testador.DTO.DadosAlertaDTO;
import com.testeBanda.testador.models.Alerta;
import com.testeBanda.testador.models.Disponibilidade;
import com.testeBanda.testador.models.Mes;
import com.testeBanda.testador.models.Queda;
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

    @Value("${testador.nagios}")
    private String nagios;

    public List<Alerta> todosAlertasDoAno(){
        String url =  nagios +"archivejson.cgi?query=alertlist&statetypes=hard&hoststates=up+down&servicestates=critical&" +
                "starttime="+getDateLimits()[0]+"&endtime="+ getDateLimits()[1];
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

    public  DadosAlertaDTO listaDeAlertas(DadosAlertaDTO dto){
        String url =  nagios +"archivejson.cgi?query=alertlist&statetypes=hard&hoststates=up+down&servicestates=critical&" +
                "starttime="+getDateLimits()[0]+"&endtime="+ getDateLimits()[1];
        JsonNode alertListNode = sendRequest(url,"alertlist");

        //Cria os alertas e add na lista
        List<Alerta> alertas = new ArrayList<>();
        if (alertListNode.isArray()) {
            for (JsonNode alert : alertListNode) {
                String name = alert.path("name").asText();
                String tipo = alert.path("plugin_output").asText();
                long timestampMillis = alert.path("timestamp").asLong();
                LocalDateTime data = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(timestampMillis),
                        TimeZone.getDefault().toZoneId()
                );
                alertas.add(new Alerta(name, data ,tipo));
            }

            //Separa as cidades, colocando alertas pelo nome da cidade.
            Map<String,ArrayList<Alerta>> mapaAlerta = new HashMap<>();
            for (Alerta item : alertas) {
                String cidade = item.getNome();
                mapaAlerta.putIfAbsent(cidade, new ArrayList<>());
                mapaAlerta.get(cidade).add(item);
            }
            ArrayList<String> durations = new ArrayList<>();
            int maiorque4 = 0;
            int atequatro = 0;
            int ateduashoras = 0;
            int umahora =0;
            int ate10m = 0;

            //Verificando cidade por cidade se tem down ou ups repitidos para fazer o calculo da queda, caso não for necessario remover, a queda conta apenas com o down...
            for (ArrayList<Alerta> alerta : mapaAlerta.values()) {
                if(alerta.isEmpty()){
                    break;
                }
                ArrayList<Alerta> up = new ArrayList<>();
                ArrayList<Alerta> down = new ArrayList<>();
                //como tem que ser em sequencia down up, se o primeiro for dowm é removido, como também se o ultimo for down, deve ser removido...
                if(alerta.getFirst().getTipo().contains("UP") ) {
                    alerta.removeFirst();
                }
                if(alerta.getLast().getTipo().contains("DOWN") ) {
                    alerta.removeLast();
                }
                //dado o if acima, sempre começara com down
                String ultimoTipo = "UP"; // começa como "UP" para esperar "DOWN" primeiro
                Iterator<Alerta> iterator = alerta.iterator();

                while (iterator.hasNext()) {
                    Alerta alert = iterator.next();
                    String tipoAtual = alert.getTipo();
                    if (tipoAtual.equals(ultimoTipo)) {
                        System.out.println("Sequência quebrada: dois '" + tipoAtual + "' seguidos. Removendo: " + alert);
                        iterator.remove();
                        continue;
                    }
                    ultimoTipo = tipoAtual;
                    if (tipoAtual.equals("UP")) {
                        up.add(alert);
                    } else if (tipoAtual.equals("DOWN")) {
                        down.add(alert);
                    }
                }

                for (int i = 0; i < up.size(); i++) {
                    Duration duration = Duration.between(down.get(i).getData(), up.get(i).getData());

                    if(duration.getSeconds() > 14400){
                        down.get(i).setTempoFora("MAIS4");
                        maiorque4++;
                    }
                    if (duration.getSeconds() >= 7200 && duration.getSeconds() < 14400 ) {
                        down.get(i).setTempoFora("ATE4");
                        atequatro++;
                    }
                    if(duration.getSeconds() >= 3600 && duration.getSeconds() < 7200){
                        down.get(i).setTempoFora("ATE2");
                        ateduashoras++;
                    }
                    if ( duration.getSeconds() >= 600 && duration.getSeconds() < 3600 ){
                        down.get(i).setTempoFora("ATE1");
                        umahora++;
                    }
                    if (duration.getSeconds() < 600 ){
                        down.get(i).setTempoFora("ATE0");
                        ate10m++;
                    }
                    durations.add("Queda em " + alerta.get(i).getNome() + " por " + duration.getSeconds() + " segundos");
                }

            }

            dto.msgTempoAleras = "<p>Total de quedas maiores que 4 horas: " + maiorque4
                    + "<br> total de quedas até 4 horas: " + atequatro
                    + "<br> total de quedas até 2 horas: " + ateduashoras
                    + "<br> total de quedas até 1 hora: " + umahora
                    + "<br> total de quedas menor que 10 minutos: " + ate10m + "</p>";

        }
            else {
            System.out.println("alertlist não é um array ou está vazio.");
        }

        //remove alertas up.
        alertas.removeIf(alerta -> alerta.getTipo().contains("UP") || alerta.getNome().isEmpty() || alerta.getNome().equals("1Teste"));
            dto.alertasDown = alertas;
            dto.setMesDisponibilidades(relatorioDeDisponibilidade());
        return dto;
    }

    public ArrayList<Mes> relatorioDeDisponibilidade(){
        ArrayList<Mes> meses = new ArrayList<>();
        Year ano = Year.now();
        Month mesPresente = LocalDate.now().getMonth();
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

    @Value("${nagios.password}")
    private String password;
    @Value("${nagios.username}")
    private String username;

    private String getAuth(){
        String auth = username + ":" + password;
        return Base64.getEncoder().encodeToString(auth.getBytes());
    }


}
