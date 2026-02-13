package com.testeBanda.testador.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

@Slf4j
@Service
public class GlpiAPI {
    @Value("${glpi.startSessionUrl}")
    private String startSessionUrl;
    @Value("${glpi.ticketUrl}")
    private String TicketUrl;
    @Value("${glpi.userToken}")
    private String user_token;
    @Value("${glpi.appToken}")
    private String appToken;
    private String tokenInUse = "";

    public GlpiAPI() {
    }

    public  void getSessionToken() throws IOException, InterruptedException {
        log.info("Requisitando novo token de sessão");
        if ( !Objects.equals(tokenInUse, "") && isTokenValido()) {
            log.info("Token ainda é válido");
            return;
        }
        log.warn("Token expirado, enviando nova requisição");
        URI uri = URI.create(startSessionUrl);
        // TODO colocar em try..catch com log para erro
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("App-Token", appToken)
                .header("Authorization", "user_token " + user_token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response;
        response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = new ObjectMapper().readTree(response.body());
        log.info("novo token {}", json.get("session_token").asText());
        this.tokenInUse = json.get("session_token").asText();
    }

    private boolean isTokenValido(){
        String url = "http://glpi.mp.rs.gov.br/apirest.php/getMyEntities";
        HttpResponse<String> response = sendHttp(url, "", true, true); // Um GET simples
        return response.statusCode() == 200;
    }

    public  String createGlpiTicket(String unidade)   {
        log.info("Criando chamado GLPI referente à unidade: {}", unidade);
        HttpResponse<String> response = sendHttp(TicketUrl, createGlpiJson(unidade),true,false);
        JsonNode json = null;
        try {
            json = new ObjectMapper().readTree(response.body());
            String ticket = json.get("id").asText();
            if(response.statusCode() == 201) {
                log.info("Chamado criado com sucesso - GLPI {}", ticket);
                return ticket;
            }
            else{
                log.error("Chamado não foi criado");
                return "";
            }
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    public  ResponseEntity<String> insertFollowUpTicket(String ticket, String content) {
        log.info("Inserindo FollowUp ao chamado {} : {}", ticket, content);
        String uri = TicketUrl + "/" + ticket + "/ITILFollowup";
        HttpResponse<String> response = sendHttp(uri, addFollowUpJson(ticket,content), true, false);
        if (response.statusCode() == 201) {
            log.info("FollowUp foi adicionado ao chamado {} com sucesso", ticket);
            return ResponseEntity.ok("FollowUp foi adicionado com sucesso");
        }
        else{
            log.error("FollowUp não foi adicionado ao chamado {}", ticket);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FollowUp não foi adicionado");
        }
    }

    // TODO Revisar todo esse método
    public ResponseEntity<String> closeGlpiTicket(String ticket){
        log.info("Fechando chamado {}", ticket);
        String uri = TicketUrl + "/" + ticket;
        JsonNode json;
        try {
            HttpResponse<String> response = sendHttp(uri, closeTicketJson(), false, false);
            json = new ObjectMapper().readTree(response.body());

            String sucess = "false";
            if (json.isArray() && json.has(0)) {
                JsonNode firstElement = json.get(0);
                sucess = firstElement.get(ticket).asText();
            }
            if(sucess.equals("true") && response.statusCode() == 200) {
                log.info("Chamado {} fechado com sucesso", ticket);
                return ResponseEntity.ok("Sucesso no fechamento do ticket");
            }
            else{
                throw new Exception("Optional error message");
                //return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Falha no fechamento do ticket");
            }
        } catch (Exception e) {
            log.error("Falha no fechamento do ticket {}", ticket, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Falha no fechamento do ticket: " + e.getMessage());
        }
    }

    private HttpResponse<String> sendHttp(String url, String json, boolean isPost, boolean passValidationToken) {
        try {
            if(!passValidationToken) {
                getSessionToken();
            }
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("App-Token", appToken)
                    .header("session-token", tokenInUse)
                    .header("Content-Type", "application/json");
            if ( isPost ) {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(json));
            } else {
                requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(json));
            }
            HttpRequest request = requestBuilder.build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error("Erro na requisição : ", e);
            throw new RuntimeException(e);
        }
    }

    public String closeTicketJson(){
        return """
                {
                  "input": {
                    "status": 6,
                    "solution": "<p>Chamado finallizado</p>",
                    "content": "<p>Chamado finallizado</p>",
                    "solutiontypes_id": 1,
                    "solutiontemplates_id": 5
                  }
                }
        """;
    }

    public String createGlpiJson(String nomeDoHost) {
        String json = """
        {
            "input": {
                "name": "Unidade sem conexão - %s",
                "content": "Unidade de %s está sem conexão de internet",
                "itilcategories_id": 472,
                "type": 1,
                "urgency": 3,
                "impact": 3,
                "priority": 3,
                "_groups_id_assign": 843,
                "entities_id": 0
            }
        }
        """;
        return String.format(json, nomeDoHost, nomeDoHost);
    }

    public String addFollowUpJson(String ticket, String content) {
        String json = """
            {
                "input": {
                    "itemtype":"Ticket",
                    "items_id":%s,
                    "content": "%s"
                }
            }
        """;
        return String.format(json, ticket, content);
    }

}
