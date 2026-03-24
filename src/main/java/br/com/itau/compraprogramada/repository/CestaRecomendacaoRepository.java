package br.com.itau.compraprogramada.repository;

import br.com.itau.compraprogramada.domain.cesta.CestaRecomendacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CestaRecomendacaoRepository extends JpaRepository<CestaRecomendacao, Long> {
    Optional<CestaRecomendacao> findByAtivoTrue();
}
