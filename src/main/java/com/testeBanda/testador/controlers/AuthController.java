package com.testeBanda.testador.controlers;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AuthController {

    @Value("${testador.versao}")
    private String versao;

    @GetMapping("/versao")
    @ResponseBody
    public String versao() {
        return versao;
    }

    @GetMapping("/error")
    public String error() {
        return "error";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error, Model model) {
        if(error != null) {
            model.addAttribute("erro", "Login ou senha incorretos!");
        }
        model.addAttribute("versao", this.versao);
        return "login";
    }

}
