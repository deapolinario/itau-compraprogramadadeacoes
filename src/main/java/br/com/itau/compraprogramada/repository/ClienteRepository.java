package br.com.itau.compraprogramada.repository;

import br.com.itau.compraprogramada.domain.cliente.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByCpf(String cpf);
    Optional<Cliente> findByCpf(String cpf);
    List<Cliente> findAllByAtivoTrue();
}
