package com.testeBanda.testador.repository;


import com.testeBanda.testador.models.Contato;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContatoRepository extends JpaRepository<Contato, Long> {

}
