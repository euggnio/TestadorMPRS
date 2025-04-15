package com.testeBanda.testador.controlers;

import com.testeBanda.testador.DTO.TesteBandaDTO;
import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.models.Contato;
import com.testeBanda.testador.repository.CidadesRepository;
import com.testeBanda.testador.repository.ContatoRepository;
import com.testeBanda.testador.service.Microtik;
import com.testeBanda.testador.service.SchedulerTeste;
import com.testeBanda.testador.utils.StatusTeste;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.List;

@Controller
public class CidadeController {


    private final CidadesRepository cidadesRepository;
    private final Microtik microtik;
    private final StatusTeste statusTeste;
    private final ContatoRepository contatoRepository;
    @Autowired
    public CidadeController(CidadesRepository cidadesRepository, Microtik microtik, StatusTeste statusTeste, ContatoRepository contatoRepository, SchedulerTeste scheduler) {
        this.cidadesRepository = cidadesRepository;

        this.microtik = microtik;
        this.statusTeste = statusTeste;
        this.contatoRepository = contatoRepository;
    }


    @PostMapping("/testeBanda")
    @ResponseBody
    public String testeBanda(@RequestBody(required = false) String idCidade) {
        if ( statusTeste.isFlag()) {
            microtik.desligar = true;
            if (!microtik.desligar) {
                statusTeste.setFlag(false);
            }
            System.out.println(statusTeste.isFlag() ? "DESLIGANDO TESTE" : "TENTATIVA DE INICIAR NOVO TESTE BLOQUEADA, JÁ HÁ TESTES SENDO EFETUADOS " + statusTeste.isFlag());
        } else {
            System.out.println("TESTE INICIALIZADO BLOQUEANDO TESTE");
            statusTeste.setFlag(true);
            microtik.testarVelocidade(idCidade);
            System.out.println("TESTE FINALIZADO LIBERANDO NOVO TESTE");
            statusTeste.setFlag(false);
        }

        return "{\"status\": \"" + microtik.desligar + "\"}"; // Retorna JSON
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamFlux() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(sequence -> microtik.cidadeAtual.toString());
    }

    @PostMapping("/atualizar")
    public String atualizarCidade(@RequestParam String telefone, @RequestParam String nome, @RequestParam String idCidade) {
        Contato contato = new Contato();
        contato.telefone = telefone;
        contato.nome = nome;
        Cidades cidade = cidadesRepository.findById(idCidade).get();
        cidade.contatos.add(contato);
        contato.cidade = cidade;
        cidadesRepository.save(cidade);
        return "redirect:cidades";
    }

    @GetMapping("/data")
    public ResponseEntity data(@RequestParam(defaultValue = "") String idCidade) {
        TesteBandaDTO dto = new TesteBandaDTO();
        Cidades c = cidadesRepository.findById(idCidade.toUpperCase()).get();
        dto.ultimoTesteBanda = c.ultimoTesteBanda;
        dto.dataUltimoTeste = c.dataUltimoTeste;
        dto.check = c.checkTesteBanda;
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/cidadesBanda")
    public String cidadesBanda(Model model) {
        List<Cidades> cidades = cidadesRepository.findAll();
        model.addAttribute("cidades", cidades);
        model.addAttribute("status", statusTeste.isFlag());
        return "cidadesBanda";
    }

    @GetMapping("/cidades")
    public String cidades(Model model) {
        List<Cidades> cidades = cidadesRepository.findAll();
        List<Contato> contatos = contatoRepository.findAll();
        model.addAttribute("contatos", contatos);
        model.addAttribute("cidades", cidades);
        System.out.println(cidades.get(0).toString());
        return "cidades";
    }

    @GetMapping("/error")
    public String error() {
        return "error";
    }

}
