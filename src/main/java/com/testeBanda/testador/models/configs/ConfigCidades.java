package com.testeBanda.testador.models.configs;

import com.testeBanda.testador.models.Cidades;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ConfigCidades {

    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @OneToOne
    private Cidades cidade;

    public boolean duplaAbordagem = false;
    public boolean bloquearTesteBanda = false;
    public boolean limitarTesteBanda = false;

    @Size(max = 2)
    public String interfaceWanID = "";
    @Size(max = 2)
    public String interfaceLanID = "";

}
