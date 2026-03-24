package br.com.itau.compraprogramada.repository;

import br.com.itau.compraprogramada.domain.custodia.ContaGrafica;
import br.com.itau.compraprogramada.domain.custodia.ContaGrafica.TipoConta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContaGraficaRepository extends JpaRepository<ContaGrafica, Long> {
    Optional<ContaGrafica> findByClienteIdAndTipo(Long clienteId, TipoConta tipo);
    Optional<ContaGrafica> findByTipo(TipoConta tipo);
    
    @Query("SELECT c FROM ContaGrafica c WHERE c.cliente.id IN :clienteIds AND c.tipo = :tipo")
    List<ContaGrafica> findAllByClienteInAndTipo(@Param("clienteIds") List<Long> clienteIds, @Param("tipo") TipoConta tipo);
}
