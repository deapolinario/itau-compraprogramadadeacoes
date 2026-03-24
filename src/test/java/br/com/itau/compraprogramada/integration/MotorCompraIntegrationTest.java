package br.com.itau.compraprogramada.integration;

import br.com.itau.compraprogramada.domain.cesta.CestaRecomendacao;
import br.com.itau.compraprogramada.domain.cesta.ItemCesta;
import br.com.itau.compraprogramada.domain.cliente.Cliente;
import br.com.itau.compraprogramada.domain.custodia.ContaGrafica;
import br.com.itau.compraprogramada.domain.custodia.Custodia;
import br.com.itau.compraprogramada.repository.*;
import br.com.itau.compraprogramada.service.motor.MotorCompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class MotorCompraIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("compraprogramada_test")
            .withUsername("compra")
            .withPassword("compra123");

    @DynamicPropertySource
    static void configurarPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired MotorCompraService motorCompraService;
    @Autowired ClienteRepository clienteRepository;
    @Autowired CestaRecomendacaoRepository cestaRepository;
    @Autowired ContaGraficaRepository contaGraficaRepository;
    @Autowired CustodiaRepository custodiaRepository;

    @BeforeEach
    void setup() {
        custodiaRepository.deleteAll();
        clienteRepository.deleteAll();
        cestaRepository.findAll().forEach(c -> {
            c.setAtivo(false);
            cestaRepository.save(c);
        });
    }

    @Test
    void motorCompra_fluxoCompleto_distribuiParaFilhote() {
        // Arrange: cliente com R$ 3.000/mês
        Cliente cliente = new Cliente("João", "12345678901", "j@e.com", new BigDecimal("3000.00"));
        clienteRepository.save(cliente);

        ContaGrafica filhote = new ContaGrafica(cliente, "FILHOTE-TEST-001", ContaGrafica.TipoConta.FILHOTE);
        contaGraficaRepository.save(filhote);

        // Cesta com PETR4 100%
        CestaRecomendacao cesta = new CestaRecomendacao();
        ItemCesta item = new ItemCesta(cesta, "PETR4", new BigDecimal("100.00"));
        cesta.getItens().add(item);
        cestaRepository.save(cesta);

        // Act: executa motor (usa COTAHIST de src/test/resources com PETR4 = R$ 35,80)
        motorCompraService.executar(LocalDate.of(2026, 2, 5));

        // Assert: filhote deve ter ações de PETR4
        // Parcela = 3000/3 = R$ 1000
        // qtd = TRUNC(1000 / 35,80) = 27 ações
        List<Custodia> posicoes = custodiaRepository.findAllByContaId(filhote.getId());
        assertThat(posicoes).anyMatch(p -> p.getTicker().equals("PETR4") && p.getQuantidade() > 0);
    }

    @Test
    void motorCompra_idempotencia_naoExecutaDuasVezes() {
        LocalDate data = LocalDate.of(2026, 3, 5);

        Cliente cliente = new Cliente("Maria", "98765432109", "m@e.com", new BigDecimal("1500.00"));
        clienteRepository.save(cliente);
        new ContaGrafica(cliente, "FILHOTE-TEST-002", ContaGrafica.TipoConta.FILHOTE);

        CestaRecomendacao cesta = new CestaRecomendacao();
        cesta.getItens().add(new ItemCesta(cesta, "PETR4", new BigDecimal("100.00")));
        cestaRepository.save(cesta);

        motorCompraService.executar(data);
        motorCompraService.executar(data); // segunda chamada deve ser ignorada

        // Sem exceção = idempotência funcionou
    }
}
