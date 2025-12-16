package com.testeBanda.testador.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Queda {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;
    private String nomeCidade;
    private LocalDateTime data;
    private Duration tempoFora;
    private boolean faltaDeLuz;
    private Long uptime;

    @Getter
    @ManyToOne
    @JsonIgnore
    private Cidades cidade;

    public Queda(String cidade, LocalDateTime data, Duration tempoFora) {
        this.nomeCidade = cidade;
        this.data = data;
        this.tempoFora = tempoFora;
        this.uptime = 0L;
        faltaDeLuz = false;
    }

    public LocalDateTime getDataUp(){
        return this.data.plus(tempoFora);
    }

    public String toString() {
        return "Queda{" +
                "cidade='" + nomeCidade + '\'' +
                ", data='" + data + '\'' +
                ", tempoFora='" + tempoFora + '\'' +
                ", faltaDeLuz='" + faltaDeLuz + '\'' +
                ", uptime='" + uptime + '\'' +
                "}";
    }

    public void setUptime(Long uptime){
        this.uptime = uptime;
        faltaDeLuz = uptime <= 660 && uptime > 0;
    }

    public String getUptimeFormatado() {
        if(this.uptime == null || this.uptime <= 0){
            return "ERRO";
        }
        Duration d = Duration.ofSeconds(this.uptime);

        long dias = d.toDays();
        long horas = d.toHoursPart();
        long minutos = d.toMinutesPart();

        StringBuilder sb = new StringBuilder();

        if (dias > 0) sb.append(dias).append(" dias ");
        if (horas > 0) sb.append(horas).append("h ");
        if (minutos > 0) sb.append(minutos).append("min");

        if (d.compareTo(Duration.ofMinutes(1)) < 0) sb.append(">1min");

        return sb.toString().trim();
    }


}
