package com.testeBanda.testador.controlers;

import com.testeBanda.testador.models.Queda;
import com.testeBanda.testador.service.QuedaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Controller
public class QuedaController {

    private final QuedaService quedaService;

    @Autowired
    public QuedaController(QuedaService quedaService) {
        this.quedaService = quedaService;
    }

    @GetMapping("/historicoQuedas")
    public String historicoQuedas(Model model) {
        List<Queda> todasQuedas = quedaService.findQuedasNoBanco();
        List<LocalDate> listaDeDatas = quedaService.listaDeDatas(todasQuedas);
        List<Queda> quedasDoDia = quedaService.filterQuedasPorDia(todasQuedas, listaDeDatas.getFirst());
        model.addAttribute("titulo", listaDeDatas.getFirst());
        model.addAttribute("quedas", quedasDoDia);
        model.addAttribute("datas", listaDeDatas);
        return "historicoQuedas";
    }

    @GetMapping("/historicoQuedas/dia/{dataString}")
    public String historicoQuedasDia(Model model, @PathVariable String dataString) {
        List<Queda> todasQuedas = quedaService.findQuedasNoBanco();
        List<LocalDate> listaDeDatas = quedaService.listaDeDatas(todasQuedas);

        LocalDate data = LocalDate.parse(dataString);
        List<Queda> quedasDoDia = quedaService.filterQuedasPorDia(todasQuedas, data);

        model.addAttribute("titulo", data);
        model.addAttribute("quedas", quedasDoDia);
        model.addAttribute("datas", listaDeDatas);
        return "historicoQuedas";
    }

    @GetMapping("/historicoQuedas/mes/{mesString}")
    public String historicoQuedasMes(Model model, @PathVariable String mesString) {
        List<Queda> todasQuedas = quedaService.findQuedasNoBanco();
        List<LocalDate> listaDeDatas = quedaService.listaDeDatas(todasQuedas);

        LocalDate data = LocalDate.now().withMonth(Integer.parseInt(mesString));
        List<Queda> quedasDoMes = quedaService.filterQuedasPorMes(todasQuedas, data.getMonth());

        model.addAttribute("titulo", data.format(DateTimeFormatter.ofPattern("MMMM", new Locale("pt", "BR"))));
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


}
