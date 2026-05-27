package com.testeBanda.testador.api;

import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.service.CidadeService;
import com.testeBanda.testador.utils.Calculos;
import com.testeBanda.testador.utils.DataAboutTest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.legrange.mikrotik.ApiConnection;
import me.legrange.mikrotik.MikrotikApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@Getter
@Slf4j
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


    public void testar(String cidadeId) {
        log.info("== Iniciando teste de banda == ");

        List<Cidades> cidades;
        cidades = ( cidadeId == null || cidadeId.isEmpty()) ?
                cidadeService.findAll() : List.of(cidadeService.findById(cidadeId));

        if(testarFalha) {
            log.info(" == Teste das unidades falhas == ");
            cidades.removeIf(cidades1 -> cidades1.checkTesteBanda);
        }

        try {
            log.info(" == Iniciando conexão no testador: " + this.ip + " == ");
            ApiConnection api = ApiConnection.connect(this.ip);
            api.login(usuario, senha);
            for (Cidades hostEmTeste : cidades) {

                if (hostEmTeste.nome.isEmpty() || desligar) {
                    log.info((desligar ? " == Solictado parada do teste == " : " == Limite de teste atingido =="));
                    dataAboutTest.setCidadeEmTeste("@END");
                    desligar = false;
                    break;
                }
                else if(hostEmTeste.bloquearTesteBanda){
                    log.info(" == Host {} bloqueado para teste == " , hostEmTeste.nome);
                }
                //inicia processo de teste
                else {
                    try {
                        dataAboutTest.setCidadeEmTeste(hostEmTeste.nome);
                        log.info(" == Iniciado teste em " + hostEmTeste.nome + " == ");
//                        if(hostEmTeste.getVelocidadeInteger() >= 50){
                            testarHosts(hostEmTeste,api);
//                        }
//                        else
//                        {
//                            System.out.println(" -- Testando banda de 20mb, ambos os lados.");
//                            result = testarAmbosLados(hostEmTeste, api);
//                            Calculos.CorrigirTesteResTesteBanda(hostEmTeste, result);
//
//                        }
                        log.info(" == Teste finalizado, resultado: " + hostEmTeste.ultimoTesteBanda);
                    }
                    catch (Exception e) {
                        log.error(" -- Erro ao testar {} ({}): {}", hostEmTeste.nome, hostEmTeste.ip, e.getMessage());
                        hostEmTeste.checkTesteBanda = false;
                        hostEmTeste.ultimoTesteBanda = " Erro no ultimo teste ";
                    }finally {
                        cidadeService.salvarDadosHost(hostEmTeste);
                        dataAboutTest.setCidadeEmTeste("@END");
                    }
                }
            }
            api.close();
        } catch (Exception e) {
            log.error("Erro geral na conexão com o roteador: {}", e.getMessage());
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
                "password=" + senha + " " +
                " local-tx-speed=" + (host.getVelocidadeInteger() +1) +"M " +
                "remote-tx-speed="+ (host.getVelocidadeInteger() +1)+"M";
        return api.execute(cmd);
    }

    public List<Map<String, String>> testarTransmit(Cidades host, ApiConnection api) throws MikrotikApiException {
        return executarBandwidthTest(host, api, "transmit");
    }

    public List<Map<String, String>> testarReceive(Cidades host, ApiConnection api) throws MikrotikApiException {
        return executarBandwidthTest(host, api, "receive");
    }

    private List<Map<String, String>> executarBandwidthTest(Cidades host, ApiConnection api, String direction) throws MikrotikApiException {
        String cmd = "/tool/bandwidth-test address=" + host.ip +
                " duration=10s " +
                "direction=" + direction +
                " protocol=tcp " +
                "user=mprs " +
                "password=" + senha;
        if(host.limitarTesteBanda){
            cmd +=  " local-tx-speed=" + (host.getVelocidadeInteger() +1) +"M " +
                    "remote-tx-speed="+ (host.getVelocidadeInteger() +1)+"M";
        }
        return api.execute(cmd);
    }

    //todo viavel colocar a porcentagem aceitavel no properties 0.91 = 9%?
    private void testarHosts(Cidades hostEmTeste, ApiConnection api) throws MikrotikApiException {
        double rx = Calculos.calcularMedia(testarTransmit(hostEmTeste,api),"tx-total-average");
        double tx = Calculos.calcularMedia(testarReceive(hostEmTeste,api),"rx-total-average");
        double velocidadeAceitavel =hostEmTeste.getVelocidadeInteger() * 0.86;
        hostEmTeste.checkTesteBanda = (rx >= velocidadeAceitavel && tx >= velocidadeAceitavel);
        hostEmTeste.ultimoTesteBanda = "RX: " + String.format("%.3f", rx) + " | " + "TX: " + String.format("%.3f", tx);
    }

}