package br.com.itau.compraprogramada.service;

import br.com.itau.compraprogramada.cotahist.CotahistParser;
import br.com.itau.compraprogramada.domain.cesta.CestaRecomendacao;
import br.com.itau.compraprogramada.domain.cesta.ItemCesta;
import br.com.itau.compraprogramada.domain.cliente.Cliente;
import br.com.itau.compraprogramada.domain.custodia.ContaGrafica;
import br.com.itau.compraprogramada.domain.custodia.Custodia;
import br.com.itau.compraprogramada.domain.motor.ExecucaoMotor;
import br.com.itau.compraprogramada.repository.*;
import br.com.itau.compraprogramada.service.motor.MotorCompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MotorCompraServiceTest {

    @Mock private ExecucaoMotorRepository execucaoMotorRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private CestaRecomendacaoRepository cestaRepository;
    @Mock private ContaGraficaRepository contaGraficaRepository;
    @Mock private CustodiaRepository custodiaRepository;
    @Mock private HistoricoOperacaoRepository historicoRepository;
    @Mock private CotahistParser cotahistParser;
    @Mock private PrecoMedioService precoMedioService;
    @Mock private FiscalService fiscalService;

    @InjectMocks
    private MotorCompraService motorCompraService;

    private LocalDate dataRef = LocalDate.of(2026, 2, 5);

    @BeforeEach
    void setup() {
        lenient().when(execucaoMotorRepository.existsByDataReferenciaAndStatus(any(), any())).thenReturn(false);
        lenient().when(execucaoMotorRepository.findByDataReferencia(any())).thenReturn(Optional.empty());
        lenient().when(execucaoMotorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void executar_jaConcluidoParaData_ignoraSilenciosamente() {
        when(execucaoMotorRepository.existsByDataReferenciaAndStatus(dataRef, ExecucaoMotor.StatusExecucao.CONCLUIDO))
                .thenReturn(true);

        motorCompraService.executar(dataRef);

        verify(clienteRepository, never()).findAllByAtivoTrue();
    }

    @Test
    void executar_semClientesAtivos_concluidoSemCompras() {
        when(clienteRepository.findAllByAtivoTrue()).thenReturn(List.of());

        motorCompraService.executar(dataRef);

        verify(cestaRepository, never()).findByAtivoTrue();
    }

    @Test
    void executar_calculaQuantidadeCorretamente() {
        // Cliente A: R$ 3.000/mês → R$ 1.000/parcela
        Cliente clienteA = criarCliente(1L, "3000.00");
        when(clienteRepository.findAllByAtivoTrue()).thenReturn(List.of(clienteA));

        CestaRecomendacao cesta = criarCesta("PETR4", "100.00");
        when(cestaRepository.findByAtivoTrue()).thenReturn(Optional.of(cesta));

        // PETR4: R$ 35,00 → TRUNC(1000 / 35) = 28 ações (todas fracionárias)
        when(cotahistParser.buscarCotacoes(any())).thenReturn(Map.of("PETR4", new BigDecimal("35.00")));

        ContaGrafica master = new ContaGrafica("MASTER-001", ContaGrafica.TipoConta.MASTER);
        master.setId(1L);
        when(contaGraficaRepository.findByTipo(ContaGrafica.TipoConta.MASTER)).thenReturn(Optional.of(master));

        Custodia saldoMaster = new Custodia(master, "PETR4");
        saldoMaster.setQuantidade(0L);
        when(custodiaRepository.findByContaIdAndTicker(1L, "PETR4")).thenReturn(Optional.of(saldoMaster));
        when(custodiaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContaGrafica filhote = new ContaGrafica(clienteA, "FILHOTE-001", ContaGrafica.TipoConta.FILHOTE);
        filhote.setId(2L);

        // Batch queries inseridas pelo fix de N+1 do Copilot
        when(contaGraficaRepository.findAllByClienteInAndTipo(anyList(), eq(ContaGrafica.TipoConta.FILHOTE)))
                .thenReturn(List.of(filhote));

        Custodia custodiaFilhote = new Custodia(filhote, "PETR4");
        when(custodiaRepository.findAllByContaInAndTickerIn(anyList(), anyList()))
                .thenAnswer(inv -> List.of(saldoMaster, custodiaFilhote));

        when(precoMedioService.calcular(any(), anyLong(), any())).thenReturn(new BigDecimal("35.00"));
        when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        motorCompraService.executar(dataRef);

        // Saldo master = 28, distribui 28 para clienteA (único cliente, 100% do total)
        ArgumentCaptor<Custodia> custodiaCaptor = ArgumentCaptor.forClass(Custodia.class);
        verify(custodiaRepository, atLeast(2)).save(custodiaCaptor.capture());
        long qtdFilhote = custodiaCaptor.getAllValues().stream()
                .filter(c -> c.getConta().getId().equals(2L))
                .mapToLong(Custodia::getQuantidade)
                .max().orElse(0L);
        assertThat(qtdFilhote).isEqualTo(28L);
    }

    @Test
    void executar_saldoMasterMaiorQueNecessidade_naoCompra() {
        Cliente clienteA = criarCliente(1L, "3000.00");
        when(clienteRepository.findAllByAtivoTrue()).thenReturn(List.of(clienteA));

        CestaRecomendacao cesta = criarCesta("PETR4", "100.00");
        when(cestaRepository.findByAtivoTrue()).thenReturn(Optional.of(cesta));
        when(cotahistParser.buscarCotacoes(any())).thenReturn(Map.of("PETR4", new BigDecimal("35.00")));

        ContaGrafica master = new ContaGrafica("MASTER-001", ContaGrafica.TipoConta.MASTER);
        master.setId(1L);
        when(contaGraficaRepository.findByTipo(ContaGrafica.TipoConta.MASTER)).thenReturn(Optional.of(master));

        // Saldo master = 50 (maior que qtd bruta = 28) → não deve disparar nova compra
        Custodia saldoMaster = new Custodia(master, "PETR4");
        saldoMaster.setQuantidade(50L);
        when(custodiaRepository.findByContaIdAndTicker(1L, "PETR4")).thenReturn(Optional.of(saldoMaster));
        when(custodiaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContaGrafica filhote = new ContaGrafica(clienteA, "FILHOTE-001", ContaGrafica.TipoConta.FILHOTE);
        filhote.setId(2L);

        when(contaGraficaRepository.findAllByClienteInAndTipo(anyList(), eq(ContaGrafica.TipoConta.FILHOTE)))
                .thenReturn(List.of(filhote));

        Custodia custodiaFilhote = new Custodia(filhote, "PETR4");
        when(custodiaRepository.findAllByContaInAndTickerIn(anyList(), anyList()))
                .thenAnswer(inv -> List.of(saldoMaster, custodiaFilhote));

        when(precoMedioService.calcular(any(), anyLong(), any())).thenReturn(new BigDecimal("35.00"));
        when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        motorCompraService.executar(dataRef);

        // Não houve compra nova na master (saldo já suficiente) — só distribuição para o filhote
        verify(historicoRepository, times(1)).save(any());
    }

    private Cliente criarCliente(Long id, String valorMensal) {
        Cliente c = new Cliente("Cliente", "12345678901", "c@e.com", new BigDecimal(valorMensal));
        c.setId(id);
        return c;
    }

    private CestaRecomendacao criarCesta(String ticker, String percentual) {
        CestaRecomendacao cesta = new CestaRecomendacao();
        cesta.setId(1L);
        ItemCesta item = new ItemCesta(cesta, ticker, new BigDecimal(percentual));
        cesta.getItens().add(item);
        return cesta;
    }
}
