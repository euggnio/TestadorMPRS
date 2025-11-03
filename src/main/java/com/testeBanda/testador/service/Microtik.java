package com.testeBanda.testador.service;

import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.repository.CidadesRepository;
import com.testeBanda.testador.utils.Calculos;
import com.testeBanda.testador.utils.DataAboutTest;
import lombok.Getter;
import me.legrange.mikrotik.ApiConnection;
import me.legrange.mikrotik.MikrotikApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@Getter
public class Microtik {

    @Value("${testador.ip}")
    public String ip;
    @Value("${testador.senha}")
    public String senha;
    @Value("${testador.usuario}")
    public String usuario;
    public boolean testarFalha = false;
    public boolean desligar;
    @Autowired
    private CidadeService cidadeService;
    @Autowired
    private DataAboutTest dataAboutTest;

    public Microtik() {
    }

    public void testar(String cidadeId) {
        System.out.println(" == Iniciando teste de banda == ");
        List<Cidades> cidades;
        cidades = ( cidadeId == null || cidadeId.isEmpty()) ?
                cidadeService.findAll() : List.of(cidadeService.findById(cidadeId));
        if(testarFalha) {
            System.out.println(" == Teste com falhas == ");
            cidades.removeIf(cidades1 -> cidades1.checkTesteBanda);
        }

        try {
            System.out.println(" == Iniciando conexão no testador: " + this.ip + " == ");
            ApiConnection api = ApiConnection.connect(this.ip);
            api.login(usuario, senha);
            for (Cidades hostEmTeste : cidades) {
                if (hostEmTeste.nome.isEmpty() || desligar) {
                    System.out.println((desligar ? " == Solictado parada do teste == " : " == Limite de teste atingido =="));
                    dataAboutTest.setCidadeEmTeste("@END");
                    desligar = false;
                    break;
                }
                //inicia processo de teste
                else {
                    List<Map<String, String>> result = List.of();
                    try {
                        dataAboutTest.setCidadeEmTeste(hostEmTeste.nome);
                        System.out.println(" -- Iniciado teste em " + hostEmTeste.nome + " " + hostEmTeste.ip + " -- ");
                        if(hostEmTeste.getVelocidadeInteger() >= 50){
                            System.out.println(" -- Testando banda acima de 50mb, lados separados! --");
                            testarHostAcimaDe50(hostEmTeste,api);
                        }
                        else
                        {
                            System.out.println(" -- Testando banda de 20mb, ambos os lados.");
                            result = testarAmbosLados(hostEmTeste, api);
                            Calculos.CorrigirTesteResTesteBanda(hostEmTeste, result);

                        }
                        System.out.println(" == Teste finalizado em:  " + hostEmTeste.nome + " == ");
                        System.out.println(" -- Resultado do teste: " + hostEmTeste.ultimoTesteBanda);

                    }
                    catch (Exception e) {
                        System.err.println(" -- Erro ao testar " + hostEmTeste.nome + " (" + hostEmTeste.ip + "): " + e.getMessage());
                        hostEmTeste.checkTesteBanda = false;
                        hostEmTeste.ultimoTesteBanda = " Erro no ultimo teste ";
                        e.printStackTrace();
                    }finally {
                        cidadeService.salvarDadosHost(hostEmTeste);
                        dataAboutTest.setCidadeEmTeste("@END");
                    }
                }
            }
            api.close();
        } catch (Exception e) {
            System.err.println("Erro geral na conexão com o roteador: " + e.getMessage());
            e.printStackTrace();
        }
        testarFalha = false;
        dataAboutTest.setCidadeEmTeste("@END");
    }

    public List<Map<String, String>> testarAmbosLados(Cidades host, ApiConnection api) throws MikrotikApiException {
        String cmd = "/tool/bandwidth-test address=" + host.ip +
                " connection-count=5 " +
                " duration=10s " +
                "direction=both " +
                "protocol=tcp " +
                "user=mprs " +
                "password=" + senha +
                " local-tx-speed=" + (host.getVelocidadeInteger() +1) +"M " +
                "remote-tx-speed="+ (host.getVelocidadeInteger() +1)+"M";
        return api.execute(cmd);
    }

    public List<Map<String, String>> testarTransmit(Cidades host, ApiConnection api) throws MikrotikApiException {
        String cmd = "/tool/bandwidth-test address=" + host.ip +
                " duration=10s " +
                "direction=transmit " +
                "protocol=tcp " +
                "user=mprs " +
                "password=" + senha +
                " local-tx-speed=" + (host.getVelocidadeInteger() +1) +"M " +
                "remote-tx-speed="+ (host.getVelocidadeInteger() +1)+"M";
        return api.execute(cmd);
    }

    public List<Map<String, String>> testarReceive(Cidades host, ApiConnection api) throws MikrotikApiException {
        String cmd = "/tool/bandwidth-test address=" + host.ip +
                " duration=10s " +
                "direction=receive " +
                "protocol=tcp " +
                "user=mprs " +
                "password=" + senha +
                " local-tx-speed=" + (host.getVelocidadeInteger() ) +"M " +
                "remote-tx-speed="+ (host.getVelocidadeInteger() )+"M";
        return api.execute(cmd);
    }

    public void testarHostAcimaDe50(Cidades hostEmTeste, ApiConnection api) throws MikrotikApiException {
        //Testa rx e tx separados, e verifica o status
        double rx = Calculos.calcularMedia(testarTransmit(hostEmTeste,api),"tx-total-average");
        double tx = Calculos.calcularMedia(testarReceive(hostEmTeste,api),"rx-total-average");
        hostEmTeste.checkTesteBanda = hostEmTeste.getVelocidadeInteger() <= 60?  (rx > 43 && tx > 43) :(rx > 91 && tx > 91);
        hostEmTeste.ultimoTesteBanda = "RX: " + String.format("%.3f", rx) + " | " + "TX: " + String.format("%.3f", tx);
    }

}