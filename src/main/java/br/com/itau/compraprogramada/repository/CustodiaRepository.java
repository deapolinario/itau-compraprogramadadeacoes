package br.com.itau.compraprogramada.repository;

import br.com.itau.compraprogramada.domain.custodia.Custodia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustodiaRepository extends JpaRepository<Custodia, Long> {
    Optional<Custodia> findByContaIdAndTicker(Long contaId, String ticker);
    List<Custodia> findAllByContaId(Long contaId);
    List<Custodia> findAllByContaIdAndQuantidadeGreaterThan(Long contaId, Long quantidade);
    
    @Query("SELECT c FROM Custodia c JOIN FETCH c.conta WHERE c.conta.id IN :contaIds AND c.ticker IN :tickers")
    List<Custodia> findAllByContaInAndTickerIn(@Param("contaIds") List<Long> contaIds, @Param("tickers") List<String> tickers);
}
