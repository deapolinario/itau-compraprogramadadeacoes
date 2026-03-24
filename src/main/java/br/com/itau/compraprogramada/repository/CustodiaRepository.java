package br.com.itau.compraprogramada.repository;

import br.com.itau.compraprogramada.domain.custodia.Custodia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustodiaRepository extends JpaRepository<Custodia, Long> {
    Optional<Custodia> findByContaIdAndTicker(Long contaId, String ticker);
    List<Custodia> findAllByContaId(Long contaId);
    List<Custodia> findAllByContaIdAndQuantidadeGreaterThan(Long contaId, Long quantidade);
}
