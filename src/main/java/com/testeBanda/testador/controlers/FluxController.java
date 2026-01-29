package com.testeBanda.testador.controlers;

import com.testeBanda.testador.models.Queda;
import com.testeBanda.testador.service.QuedaService;
import com.testeBanda.testador.utils.DataAboutTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class FluxController {

    private final DataAboutTest dataAboutTest;
    private final QuedaService quedaService;

    @Autowired
    public FluxController(DataAboutTest dataAboutTest, QuedaService quedaService) {
        this.dataAboutTest = dataAboutTest;
        this.quedaService = quedaService;
    }


    private record TestStatus(String nome, String velocidade, Boolean statusTeste) {}

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<TestStatus> streamFlux() {
        return Flux.interval(Duration.ofSeconds(1))
                .onBackpressureDrop()
                .map(sequence -> new TestStatus(dataAboutTest.getCidadeEmTeste(), "0",dataAboutTest.isFlag()));
    }

    @GetMapping(path = "/novas_quedas", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<List<Queda>> streamQuedas() {
        return Flux.interval(Duration.ofSeconds(10))
                .onBackpressureDrop()
                .map(sequence -> quedaService.filterQuedasPorDia(quedaService.findQuedasNoBanco(), LocalDate.now()));
    }

}
