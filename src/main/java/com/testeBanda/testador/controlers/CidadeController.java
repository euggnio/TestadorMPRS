package com.testeBanda.testador.controlers;

import com.testeBanda.testador.DTO.DadosAlertaDTO;
import com.testeBanda.testador.models.*;
import com.testeBanda.testador.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.InetAddress;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class CidadeController {

    private final CidadeService cidadeService;
    private final Microtik microtik;
    private final NagiosAPI nagiosAPI;
    private final GraficosService graficoService;

    @Autowired
    public CidadeController(CidadeService cidadeService, Microtik microtik, NagiosAPI nagiosAPI, GraficosService graficosService) {
        this.cidadeService = cidadeService;
        this.microtik = microtik;
        this.nagiosAPI = nagiosAPI;
        this.graficoService = graficosService;
    }

    @GetMapping("/unidade/{city}")
    public String unidade(@PathVariable String city, Model model) {
        Cidades cidade = cidadeService.findById(city);
        if ( cidade == null) {
            cidade = new Cidades();
            cidade.nome = "Canoas";
            cidade.nomeSistema = "Canoas";
        }
        String cacti = graficoService.cacti(cidade.cacti);
        String smoke = graficoService.pegarUnidadeSmoke(cidade.nomeSistema);
        model.addAttribute("smoke", smoke);
        model.addAttribute("cacti", cacti);
        model.addAttribute("cidade", cidade);
        return "unidade";
    }

    //TODO Isso é utilizado?
    @PostMapping("/atualizar")
    public String atualizarCidade(Cidades cidade, RedirectAttributes redirectAttrs) {
        if ( cidade.nomeSistema.isEmpty() || cidade.ip.isEmpty()) {
            redirectAttrs.addFlashAttribute("status", "Falta de dados! (Obgt. nome e IP)");
            return "redirect:configuracao";
        }
        cidadeService.salvarCidade(cidade);
        redirectAttrs.addFlashAttribute("status", "Cidade salva !");
        return "redirect:configuracao";
    }

    @PostMapping("/apagarCidade")
    public String apagarCidade(Cidades cidade, RedirectAttributes redirectAttrs) {
        cidadeService.apagarCidade(cidade.nome);
        redirectAttrs.addFlashAttribute("status", "Cidade apagada !");
        return "redirect:configuracao";
    }

    @GetMapping("/pegarListaCidades")
    @ResponseBody
    public List<String> pegarListaCidades() {
        List<Cidades> cidades = cidadeService.findAll();
        return cidades.stream().map(Cidades::getNome).collect(Collectors.toList());
    }

    @GetMapping("/cidadesBanda")
    public String cidadesBanda(Model model) {
        List<Cidades> cidades = cidadeService.findAll();
        model.addAttribute("cidades", cidades);
        return "cidadesBanda";
    }

    @GetMapping("/dispositivos")
    public String dispositivos(Model model) {
        List<Cidades> cidades = cidadeService.findAll();
        model.addAttribute("cidades", cidades);
        return "dispositivos";
    }

    @GetMapping("/cidades")
    public String cidades(Model model) {
        List<Cidades> cidades = cidadeService.findAll();
        List<Contato> contatos = cidadeService.findAllContatos();
        model.addAttribute("contatos", contatos);
        model.addAttribute("cidades", cidades);
        return "cidades";
    }

    @GetMapping("/grafico")
    public String grafico(Model model){
        DadosAlertaDTO dados = new DadosAlertaDTO();
        model.addAttribute("alertas",nagiosAPI.listaDeAlertas(dados));
        return "grafico";
    }

    @GetMapping("/configuracao")
    public String configuracao(Model model, HttpServletRequest request){
        String info = request.getRemoteAddr();
        System.out.println(info);
        String hostName = "Desconhecido";
        try {
            hostName = InetAddress.getByName(info).getHostName();
        } catch (Exception e) {
            hostName = "Erro ao resolver hostname";
        }
        System.out.println(hostName);
        model.addAttribute("ip", microtik.ip);
        model.addAttribute("usuario", microtik.usuario);
        model.addAttribute("senha", microtik.senha);
        model.addAttribute("hosts", cidadeService.findAll());
        return "configuracao";
    }

    @GetMapping("/error")
    public String error() {
        return "error";
    }

    @GetMapping("/login")
    public String login() {
        return "cidadesBanda";
    }

}
