package com.testeBanda.testador.controlers;


import com.testeBanda.testador.api.SnmpWanMonitor;
import com.testeBanda.testador.models.ResultadosSnmp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;


@Controller
public class SnmpController {

    private final SnmpWanMonitor monitor;

    @Autowired
    public SnmpController(SnmpWanMonitor monitor) {
        this.monitor = monitor;
    }

    @GetMapping("/snmpWan")
    @ResponseBody
    public List<ResultadosSnmp> getSnmpList2(){
        System.out.println("getSnmpList2");
        return monitor.resultados;
    }


}
