package com.testeBanda.testador.controlers;

import com.testeBanda.testador.models.Queda;
import com.testeBanda.testador.repository.QuedaRepository;
import com.testeBanda.testador.service.GlpiService;
import com.testeBanda.testador.service.QuedaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
public class QuedaController {

    private final QuedaService quedaService;
    private final GlpiService glpiService;
    private final QuedaRepository quedaRepository;

    @Value("#{'${glpi.usuarios.nome}'.split(',')}")
    private List<String> responsaveis;
    @Value("#{'${glpi.usuarios.id}'.split(',')}")
    private List<String> responsaveisid;

    @Autowired
    public QuedaController(QuedaService quedaService, GlpiService glpiService, QuedaRepository quedaRepository) {
        this.quedaService = quedaService;
        this.glpiService = glpiService;
        this.quedaRepository = quedaRepository;
    }

    @GetMapping("/historicoQuedas")
    public String historicoQuedas(Model model) {
        LocalDate data = LocalDate.now();

        List<LocalDate> listaDeDatas = quedaService.findListaDatas();
        List<Queda> quedasDoDia = quedaService.findQuedasDoDiaAtual(data);

        model.addAttribute("responsaveis", responsaveis);
        model.addAttribute("responsaveisid", responsaveisid);
        model.addAttribute("titulo", data);
        model.addAttribute("quedas", quedasDoDia);
        model.addAttribute("datas", listaDeDatas);
        return "historicoQuedas";
    }

    @GetMapping("/historicoQuedas/dia/{dataString}")
    public String historicoQuedasDia(Model model, @PathVariable String dataString) {
        LocalDate data = LocalDate.parse(dataString);

        List<LocalDate> listaDeDatas = quedaService.findListaDatas();
        List<Queda> quedasDoDia = quedaService.findQuedasDoDia(data);

        model.addAttribute("responsaveis", responsaveis);
        model.addAttribute("responsaveisid", responsaveisid);
        model.addAttribute("titulo", data);
        model.addAttribute("quedas", quedasDoDia);
        model.addAttribute("datas", listaDeDatas);
        return "historicoQuedas";
    }

    @GetMapping("/historicoQuedas/mes/{ano}/{mes}")
    public String historicoQuedasMes(Model model, @PathVariable int ano, @PathVariable String mes) {
        LocalDate data = LocalDate.now().withMonth(Integer.parseInt(mes)).withYear(ano);

        List<LocalDate> listaDeDatas = quedaService.findListaDatas();
        List<Queda> quedasDoMes = quedaService.findQuedasDoMes(ano, data.getMonth());

        String mesPTBR = data.format(DateTimeFormatter.ofPattern("MMMM", new Locale("pt", "BR")));
        String tituloString = mesPTBR.substring(0,1).toUpperCase() + mesPTBR.substring(1) + " " + ano;

        model.addAttribute("responsaveis", responsaveis);
        model.addAttribute("responsaveisid", responsaveisid);
        model.addAttribute("titulo", tituloString);
        model.addAttribute("quedas", quedasDoMes);
        model.addAttribute("datas", listaDeDatas);

        return "historicoQuedas";
    }

    @PostMapping("/editaFaltaDeLuz")
    public String editaFaltaDeLuz(@ModelAttribute("id") long quedaId,
                                  @ModelAttribute("faltaDeLuz") boolean novoValor,
                                  @ModelAttribute("paginaAtual") String paginaAtual){
        quedaService.editaFaltaDeLuz(quedaId, novoValor);
        return "redirect:" + paginaAtual;
    }

    @PostMapping("/editarProtocolo/{id}")
    public ResponseEntity<String> editarProtocolo(@PathVariable long id, @RequestBody String data){
        if ( data.length() > 10 ){
            System.out.println(data);
            return ResponseEntity.badRequest().body("Data não pode ser maior que 10 caracteres");
        }
        glpiService.editarProtocolo(id, data);
        return ResponseEntity.ok().body("Protocolo editado com sucesso!");
    }

    @PostMapping("/adicionarFollowUp/{id}")
    public ResponseEntity<String> editarTicketGlpi(@PathVariable long id, @RequestBody String data){
        glpiService.adicionarFollowUp(id, data);
        return ResponseEntity.ok().body("FollowUp adicionado com sucesso!");
    }

    @PostMapping("/fecharChamado/{id}")
    public ResponseEntity<String> fecharChamado(@PathVariable long id, @RequestBody(required = false) String data){
        glpiService.fecharChamado(id, data);
        return ResponseEntity.ok().body("Chamado fechado com sucesso!");
    }

    @PostMapping("/abrirChamado/{id}")
    public ResponseEntity<String> abrirChamado(@PathVariable long id){
        glpiService.abrirChamado(id);
        return ResponseEntity.ok().body("Chamado criado com sucesso!");
    }

    @PostMapping("/atribuirResponsavel/{id}")
    public ResponseEntity<String> atribuirResponsavel(@PathVariable long id, @RequestBody String data){
        glpiService.atribuirResponsavel(id,data);
        return ResponseEntity.ok().body("Responsável adicionado com sucesso!");
    }

    @GetMapping("/getTicketFollowups/{id}")
    public ResponseEntity<List<String>> getTicketFollowups(@PathVariable long id){
        return ResponseEntity.ok(glpiService.getFollowUpTicket(id));
    }

    @GetMapping("/testarOnline")
    @ResponseBody
    public void testarOnline(){
        Optional<Queda> queda = quedaRepository.findById(56163L);
        Queda queda1 = queda.get();
        System.out.println(queda1);
        System.out.println(queda1.getTempoFora());
    }
}
