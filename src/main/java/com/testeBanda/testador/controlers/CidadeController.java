package com.testeBanda.testador.controlers;

import com.testeBanda.testador.DTO.DadosAlertaDTO;
import com.testeBanda.testador.api.Microtik;
import com.testeBanda.testador.api.NagiosAPI;
import com.testeBanda.testador.models.*;
import com.testeBanda.testador.repository.CidadesRepository;
import com.testeBanda.testador.repository.DispositivosRepository;
import com.testeBanda.testador.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class CidadeController {

    private final ScanService scanService;
    private final CidadesRepository cidadesRepository;
    private final EventosService eventosService;
    private final CidadeService cidadeService;
    private final Microtik microtik;
    private final GraficosService graficoService;
    private final QuedaService quedaService;
    private final NagiosAPI nagiosAPI;
    private final DispositivosRepository dispositivosRepository;
    private final NagiosSshService nagiosSshService;

    @Autowired
    public CidadeController(CidadeService cidadeService, Microtik microtik, GraficosService graficosService, QuedaService quedaService, NagiosAPI nagiosAPI, DispositivosRepository dispositivosRepository, ScanService scanService, CidadesRepository cidadesRepository, NagiosSshService nagiosSshService, EventosService eventosService) {
        this.cidadeService = cidadeService;
        this.microtik = microtik;
        this.graficoService = graficosService;
        this.quedaService = quedaService;
        this.nagiosAPI = nagiosAPI;
        this.dispositivosRepository = dispositivosRepository;
        this.scanService = scanService;
        this.cidadesRepository = cidadesRepository;
        this.nagiosSshService = nagiosSshService;
        this.eventosService = eventosService;
    }



    @GetMapping("/unidade/{city}")
    public String unidade(@PathVariable String city, Model model) {
        Cidades cidade = cidadeService.findByNagios(city);

        if ( cidade == null) {
            cidade = cidadeService.findById(city);
        }

        String cacti = graficoService.cacti(cidade.cacti);
        String smoke = graficoService.pegarUnidadeSmoke(cidade.smokeID);
        System.out.println(cidade);
        model.addAttribute("quedas", quedaService.findQuedasNoBanco());
        model.addAttribute("smoke", smoke);
        model.addAttribute("cacti", cacti);
        model.addAttribute("cidade", cidade);
        model.addAttribute("dispositivos", cidade.getDispositivos());
        return "unidade";
    }

    @GetMapping("/testes")
    public String testes(Model model) throws IOException, InterruptedException {
        List<Cidades> cidades = cidadeService.findAll();
        nagiosAPI.preencherCoordenadas(cidades);
        return "historicoQuedas";
    }

    @CrossOrigin(origins = "https://nagiosmpls.mp.rs.gov.br")
    @GetMapping("/pegarGraficoSmoke/{id}")
    public ResponseEntity<String> pegarGraficoSmoke(@PathVariable String id) {
        return ResponseEntity.ok(graficoService.pegarUnidadeSmoke(id));
    }

    @PostMapping("/atualizar")
    public String atualizarCidade(Cidades cidade, RedirectAttributes redirectAttrs) {
        if ( cidade.nome == null || cidade.nome.isEmpty() || cidade.ip == null || cidade.ip.isEmpty()) {
            redirectAttrs.addFlashAttribute("status", "Erro: Nome e IP são obrigatórios.");
            return "redirect:configuracao";
        }

        String ip = cidade.getIp().trim();
        if (!ip.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            redirectAttrs.addFlashAttribute("status", "Erro: Formato de IP inválido.");
            return "redirect:configuracao";
        }
//        if (ip.endsWith(".1")) {
//            redirectAttrs.addFlashAttribute("status", "Erro: IP não pode ser de roteador (termina em .1).");
//            return "redirect:configuracao";
//        }

        Optional<Cidades> existentePorIp = cidadesRepository.findByIp(ip);
        if (existentePorIp.isPresent()) {
            String nomeExistente = existentePorIp.get().getNome();
            String nomeAtual = cidade.getNome() != null ? cidade.getNome().trim() : "";
            if (!nomeExistente.equalsIgnoreCase(nomeAtual)) {
                redirectAttrs.addFlashAttribute("status", "Erro: Já existe uma cidade com o IP " + ip + ".");
                return "redirect:configuracao";
            }
        }

        if (cidade.getNotacao() != null && !cidade.getNotacao().isEmpty()) {
            try {
                int notacao = Integer.parseInt(cidade.getNotacao());
                if (notacao < 23 || notacao > 30) {
                    redirectAttrs.addFlashAttribute("status", "Erro: Notação deve ser entre /23 e /30.");
                    return "redirect:configuracao";
                }
            } catch (NumberFormatException e) {
                redirectAttrs.addFlashAttribute("status", "Erro: Notação deve ser um número.");
                return "redirect:configuracao";
            }
        }

        if (cidade.getVlans() != null && !cidade.getVlans().isBlank()) {
            for (String vlan : cidade.getVlans().split(",")) {
                String trimmed = vlan.trim();
                if (trimmed.isEmpty()) continue;
                String[] partes = trimmed.split("/");
                if (partes.length != 2) {
                    redirectAttrs.addFlashAttribute("status", "Erro: VLAN '" + trimmed + "' inválida. Formato: IP/CIDR (ex: 172.17.1.224/29)");
                    return "redirect:configuracao";
                }
                try {
                    int cidr = Integer.parseInt(partes[1].trim());
                    if (cidr < 1 || cidr > 30) {
                        redirectAttrs.addFlashAttribute("status", "Erro: VLAN '" + trimmed + "' com CIDR inválido.");
                        return "redirect:configuracao";
                    }
                } catch (NumberFormatException e) {
                    redirectAttrs.addFlashAttribute("status", "Erro: VLAN '" + trimmed + "' com CIDR não numérico.");
                    return "redirect:configuracao";
                }
            }
        }

        Cidades cidadeModificar;
        Optional<Cidades> cidadeExiste = cidadeService.findByIdOptional(cidade.getNome());
        cidadeModificar = cidadeExiste.orElseGet(Cidades::new);
        cidadeModificar.setNome(cidade.getNome().trim());
        cidadeModificar.setIp(ip);
        cidadeModificar.setCodigo(cidade.codigo);
        cidadeModificar.setVelocidade(cidade.velocidade);
        cidadeModificar.setIntra(cidade.intra);
        cidadeModificar.setNotacao(cidade.notacao);
        cidadeModificar.setVlans(cidade.getVlans());
        cidadeModificar.setNagiosID(cidade.nagiosID);
        cidadeModificar.setSmokeID(cidade.smokeID);
        cidadeModificar.setCacti(cidade.cacti);

        cidadeModificar.checkTesteBanda = false;
        cidadeModificar.getConfig().setBloquearTesteBanda(cidade.getConfig().bloquearTesteBanda);
        cidadeModificar.getConfig().setLimitarTesteBanda(cidade.getConfig().limitarTesteBanda);
        cidadeModificar.getConfig().setDuplaAbordagem(cidade.getConfig().duplaAbordagem);
        cidadeModificar.getConfig().setTestarUDP(cidade.getConfig().testarUDP);
        cidadeModificar.getConfig().setInterfaceLanID(cidade.getConfig().interfaceLanID);
        cidadeModificar.getConfig().setInterfaceWanID(cidade.getConfig().interfaceWanID);

        cidadeService.salvarCidade(cidadeModificar);
        redirectAttrs.addFlashAttribute("status", "Cidade salva com sucesso!");
        return "redirect:configuracao";
    }

    @PostMapping("/apagarCidade")
    public String apagarCidade(Cidades cidade, RedirectAttributes redirectAttrs) {
        cidadeService.apagarCidade(cidade.nome);
        redirectAttrs.addFlashAttribute("status", "Cidade apagada !");
        return "redirect:configuracao";
    }

    @GetMapping("/verificarIp")
    @ResponseBody
    public Map<String, Object> verificarIp(@RequestParam String ip, @RequestParam(required = false) String nomeAtual) {
        Map<String, Object> resultado = new HashMap<>();
        Optional<Cidades> existente = cidadesRepository.findByIp(ip);
        if (existente.isPresent() && !existente.get().getNome().equals(nomeAtual)) {
            resultado.put("existe", true);
            resultado.put("cidade", existente.get().getNome());
        } else {
            resultado.put("existe", false);
        }
        return resultado;
    }

    @GetMapping("/varreduraFlaps")
    @ResponseBody
    public String teste() {
        quedaService.identificaitorDeFlaps();
        return "varreduraFlaps OK";
    }

    @GetMapping("/testarDebug")
    @ResponseBody
    public NagiosSshService.Resultado configurar() throws Exception {
        return nagiosSshService.cadastrarLinkPrimario("Alvorada","TESTER13123","188.784.475.425");
    }

    @GetMapping("/pegarListaCidades")
    @ResponseBody
    public List<String> pegarListaCidades() {
        List<Cidades> cidades = cidadeService.findAll();
        return cidades.stream().map(Cidades::getNome).collect(Collectors.toList());
    }

    @GetMapping({"/cidadesBanda", "/"})
    public String cidadesBanda(Model model) {
        List<Cidades> cidades = cidadeService.findAll();
        model.addAttribute("cidades", cidades);
        return "cidadesBanda";
    }

    @GetMapping("/dispositivos")
    public String dispositivos(Model model) {
        List<Dispositivos> dispositivos = dispositivosRepository.findAll();
        model.addAttribute("dispositivos", dispositivos);
        return "dispositivos";
    }

    @GetMapping("/cidades")
    public String cidades(Model model) {
        List<Cidades> cidades = cidadeService.findAll();

        model.addAttribute("quedas", quedaService.findQuedasNoBanco());
        model.addAttribute("cidades", cidades);
        return "cidades";
    }

    @GetMapping("/grafico")
    public String grafico(Model model){
        DadosAlertaDTO dados = new DadosAlertaDTO();
        model.addAttribute("alertas", quedaService.PreencherDTO(dados, Year.now().getValue()));
        model.addAttribute("eventos", eventosService.pegarTodos());
        return "grafico";
    }

    @GetMapping("/grafico/{ano}")
    public String graficoAno(Model model, @PathVariable String ano){
        DadosAlertaDTO dados = new DadosAlertaDTO();
        model.addAttribute("alertas", quedaService.PreencherDTO(dados, Integer.parseInt(ano)));
        model.addAttribute("eventos", eventosService.pegarTodos());
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
        model.addAttribute("hosts", cidadeService.findAll());
        model.addAttribute("eventos", eventosService.pegarTodos());
        return "configuracao";
    }



}
