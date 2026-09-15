package com.testeBanda.testador.controlers;

import com.testeBanda.testador.api.SnmpSwitches;
import com.testeBanda.testador.api.SnmpWanMonitor;
import com.testeBanda.testador.models.ResultadosSnmp;
import com.testeBanda.testador.models.SwitchStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;


@Controller
public class SnmpTestController {

    private final SnmpSwitches swMonitor;

    @Autowired
    public SnmpTestController(SnmpSwitches swMonitor) {
        this.swMonitor = swMonitor;
    }

    @GetMapping("/snmpSwitch/{ip}")
    @ResponseBody
    public SwitchStatus getSnmpSwitch(@PathVariable String ip) throws Exception {
        return swMonitor.getInfo(ip);
    }

    @GetMapping("/switchStatus")
    public String getSnmpSwitch(Model model){
        return "switchStatus";
    }



}
