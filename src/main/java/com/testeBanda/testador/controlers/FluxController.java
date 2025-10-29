package com.testeBanda.testador.controlers;

import com.testeBanda.testador.utils.DataAboutTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Controller
public class FluxController {

    private final DataAboutTest dataAboutTest;
    @Autowired
    public FluxController( DataAboutTest dataAboutTest) {
        this.dataAboutTest = dataAboutTest;
    }


    private record TestStatus(String nome, String velocidade, Boolean statusTeste) {}

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<TestStatus> streamFlux() {
        return Flux.interval(Duration.ofSeconds(1))
                .onBackpressureDrop()
                .map(sequence -> new TestStatus(dataAboutTest.getCidadeEmTeste(), "0",dataAboutTest.isFlag()));
    }

}
