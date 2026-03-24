package br.com.itau.compraprogramada.repository;

import br.com.itau.compraprogramada.domain.motor.ExecucaoMotor;
import br.com.itau.compraprogramada.domain.motor.ExecucaoMotor.StatusExecucao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExecucaoMotorRepository extends JpaRepository<ExecucaoMotor, Long> {
    Optional<ExecucaoMotor> findByDataReferencia(LocalDate dataReferencia);
    boolean existsByDataReferenciaAndStatus(LocalDate dataReferencia, StatusExecucao status);
}
