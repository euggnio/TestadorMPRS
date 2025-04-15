package com.testeBanda.testador.service;

import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.repository.CidadesRepository;
import com.testeBanda.testador.utils.Calculos;
import lombok.Getter;
import me.legrange.mikrotik.ApiConnection;
import me.legrange.mikrotik.MikrotikApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Getter
public class Microtik {

    @Autowired
    private CidadesRepository cidadesRepository;
    public boolean desligar;
    public Cidades cidadeAtual;

    public Microtik() {
        cidadeAtual = new Cidades();
        cidadeAtual.nome = "@END";
    }

    public void testarVelocidade(String cidadeId) {
        System.out.println(" == Iniciando teste de banda == ");
        List<Cidades> cidades;

        cidades = ( cidadeId == null ) ?
                cidadesRepository.findAll() : List.of(cidadesRepository.findById(cidadeId).get());

        String routerIP = "888.88.88.1";
        String username = "********";
        String password = "********";

        //===========OK=============

        try {
            System.out.println(" == Iniciando conexão no testador: " + routerIP + " == ");
            ApiConnection api = ApiConnection.connect(routerIP);
            api.login(username, password);
            //===========OK=============
            for (Cidades hostEmTeste : cidades) {
                //Verificar desligamento ou limitar até cidade com nome x
                if (hostEmTeste.nome.isEmpty() || desligar) {
                    System.out.println(" == " + (desligar ? "SOLICITADO PARADA DO TESTE." : "LIMITE DE TESTE ATINGIDO") + " ==");
                    this.cidadeAtual.nome = "@END";
                    desligar = false;
                    break;
                }
                //inicia processo de teste
                else {
                    try {
                        cidadeAtual = hostEmTeste;
                        System.out.println(" == Teste em " + hostEmTeste.nome + " " + hostEmTeste.ip + " == ");
                        List<Map<String, String>> result = List.of();
                        if(hostEmTeste.getVelocidadeByCity() >= 50){
                            System.out.println(" == Teste em velocidades separadas! == ");
                            processarResultadoAcima50(hostEmTeste,api);
                        }
                        else
                        {
                            result = testar(hostEmTeste, api);
                            atualizarResultadoNoHost(hostEmTeste, result);
                        }
                        System.out.println(" == Teste finalizado em:  " + hostEmTeste.nome + " == ");
                    } catch (Exception e) {
                        System.err.println("Erro ao testar " + hostEmTeste.nome + " (" + hostEmTeste.ip + "): " + e.getMessage());
                        hostEmTeste.checkTesteBanda = false;
                        hostEmTeste.ultimoTesteBanda = " ERRO NO ULTIMO TESTE ";
                        cidadeAtual = hostEmTeste;
                        cidadesRepository.save(hostEmTeste);
                        e.printStackTrace();
                    }
                }
            }
            api.close();
            this.cidadeAtual.nome = "@END";
        } catch (Exception e) {
            System.err.println("Erro geral na conexão com o roteador: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Map<String, String>> testar(Cidades host, ApiConnection api) throws MikrotikApiException {
        String cmd = "/tool/bandwidth-test address=" + host.ip +
                " connection-count=2 " +
                " duration=10s " +
                "direction=both " +
                "protocol=tcp " +
                "user=mprs " +
                "password=Vnld0p$ " +
                "local-tx-speed=" + (host.getVelocidadeByCity() +1) +"M " +
                "remote-tx-speed="+ (host.getVelocidadeByCity() +1)+"M";
        return api.execute(cmd);
    }

    public List<Map<String, String>> testarTransmit(Cidades host, ApiConnection api) throws MikrotikApiException {
        String cmd = "/tool/bandwidth-test address=" + host.ip +
                " duration=10s " +
                "direction=transmit " +
                "protocol=tcp " +
                "user=mprs " +
                "password=Vnld0p$ " +
                "local-tx-speed=" + (host.getVelocidadeByCity() +1) +"M " +
                "remote-tx-speed="+ (host.getVelocidadeByCity() +1)+"M";
        return api.execute(cmd);
    }

    public List<Map<String, String>> testarReceive(Cidades host, ApiConnection api) throws MikrotikApiException {
        String cmd = "/tool/bandwidth-test address=" + host.ip +
                " duration=10s " +
                "direction=receive " +
                "protocol=tcp " +
                "user=mprs " +
                "password=Vnld0p$ " +
                "local-tx-speed=" + (host.getVelocidadeByCity() ) +"M " +
                "remote-tx-speed="+ (host.getVelocidadeByCity() )+"M";
        return api.execute(cmd);
    }

    public void processarResultadoAcima50(Cidades hostEmTeste, ApiConnection api) throws MikrotikApiException {
        double rx = Calculos.calcularMedia(testarTransmit(hostEmTeste,api),"tx-total-average");
        double tx = Calculos.calcularMedia(testarReceive(hostEmTeste,api),"rx-total-average");
        hostEmTeste.checkTesteBanda = hostEmTeste.getVelocidadeByCity() <= 60?  (rx > 43 && tx > 43) :(rx > 91 && tx > 91);
        hostEmTeste.ultimoTesteBanda = "RX: " + String.format("%.3f", rx) + " | " + "TX: " + String.format("%.3f", tx);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        hostEmTeste.dataUltimoTeste = formatter.format(LocalDateTime.now());
        cidadesRepository.save(hostEmTeste);
    }

    public void atualizarResultadoNoHost(Cidades host, List<Map<String, String>> resultado) {
        Calculos.CorrigirTesteResTesteBanda(host, resultado);
        cidadeAtual = host;
        cidadesRepository.save(cidadeAtual);
    }
}