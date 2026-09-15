package com.testeBanda.testador.controlers;

import com.testeBanda.testador.DTO.EventoDTO;
import com.testeBanda.testador.service.EventosService;
import com.testeBanda.testador.service.SyslogSshService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class EventoController {

    @Autowired
    EventosService eventosService;

    @Autowired
    SyslogSshService service;

    @PostMapping("/evento/criar")
    public String criarEvento(
            @Valid EventoDTO evento,
            BindingResult erros,
            RedirectAttributes redirectAttributes) {

        if (erros.hasErrors()) {
            redirectAttributes.addFlashAttribute("status", "Erro ao criar evento!");
            return "redirect:/configuracao";
        }

        eventosService.criarEventoByDTO(evento);

        redirectAttributes.addFlashAttribute("status", "Evento salvo!");
        return "redirect:/configuracao";
    }

    @PostMapping("/evento/apagar")
    public String apagarEvento(Long id, RedirectAttributes redirectAttributes) {
        eventosService.apagarEvento(id);
        redirectAttributes.addFlashAttribute("status", "Evento apagado!");
        return "redirect:/configuracao";
    }

    @GetMapping(value = "/teste/eventos", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String logs() {
        return service.buscarLogsHtml();
    }

    @GetMapping(value = "/teste/eventos/cidade/{nome}", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String logsPorCidade(
            @PathVariable String nome,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim) {
        return service.buscarLogsPorCidade(nome, dataInicio, dataFim);
    }

    @GetMapping(value = "/teste/eventos/cache-info", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<Map<String, Object>> cacheInfo() {
        return service.infosCache();
    }

}
