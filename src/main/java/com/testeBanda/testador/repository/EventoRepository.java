package com.testeBanda.testador.repository;

import com.testeBanda.testador.models.Eventos;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoRepository extends JpaRepository<Eventos,Long> {
}
