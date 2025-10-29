package com.testeBanda.testador.repository;

import com.testeBanda.testador.models.Resultados;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultadosRepository extends JpaRepository<Resultados,Long> {

    @Query(value = """
        DELETE FROM resultados
        WHERE cidade_nome = :cidadeId
        AND id NOT IN (
            SELECT id FROM (
                SELECT id FROM resultados
                WHERE cidade_nome = :cidadeId
                ORDER BY data_teste DESC
                LIMIT 30
            ) AS tmp
        )
        """, nativeQuery = true)
    @Modifying
    @Transactional
    void deleteExcedentes(@Param("cidadeId") String cidadeId);
}
