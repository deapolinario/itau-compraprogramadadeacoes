package br.com.itau.compraprogramada.service;

import br.com.itau.compraprogramada.domain.cliente.Cliente;
import br.com.itau.compraprogramada.domain.custodia.ContaGrafica;
import br.com.itau.compraprogramada.dto.AdesaoRequest;
import br.com.itau.compraprogramada.dto.ClienteResponse;
import br.com.itau.compraprogramada.exception.NegocioException;
import br.com.itau.compraprogramada.repository.ClienteRepository;
import br.com.itau.compraprogramada.repository.ContaGraficaRepository;
import br.com.itau.compraprogramada.repository.CustodiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ContaGraficaRepository contaGraficaRepository;

    @Mock
    private CustodiaRepository custodiaRepository;

    @InjectMocks
    private ClienteService clienteService;

    private AdesaoRequest requestValida;

    @BeforeEach
    void setup() {
        requestValida = new AdesaoRequest();
        requestValida.setNome("João Silva");
        requestValida.setCpf("12345678901");
        requestValida.setEmail("joao@email.com");
        requestValida.setValorMensal(new BigDecimal("3000.00"));
    }

    @Test
    void aderir_comDadosValidos_criaClienteEConta() {
        when(clienteRepository.existsByCpf("12345678901")).thenReturn(false);
        when(clienteRepository.save(any())).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(contaGraficaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponse response = clienteService.aderir(requestValida);

        assertThat(response.getNome()).isEqualTo("João Silva");
        assertThat(response.getAtivo()).isTrue();
        assertThat(response.getNumeroConta()).startsWith("FILHOTE-");
        verify(contaGraficaRepository).save(any(ContaGrafica.class));
    }

    @Test
    void aderir_comCpfDuplicado_lancaConflict() {
        when(clienteRepository.existsByCpf("12345678901")).thenReturn(true);

        NegocioException ex = catchThrowableOfType(
                () -> clienteService.aderir(requestValida),
                NegocioException.class
        );

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).contains("CPF já cadastrado");
    }

    @Test
    void sair_clienteAtivo_desativa() {
        Cliente cliente = new Cliente("João", "12345678901", "j@e.com", BigDecimal.valueOf(1000));
        cliente.setId(1L);
        cliente.setAtivo(true);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any())).thenReturn(cliente);

        clienteService.sair(1L);

        assertThat(cliente.getAtivo()).isFalse();
    }

    @Test
    void sair_clienteJaInativo_lancaConflict() {
        Cliente cliente = new Cliente("João", "12345678901", "j@e.com", BigDecimal.valueOf(1000));
        cliente.setId(1L);
        cliente.setAtivo(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        NegocioException ex = catchThrowableOfType(
                () -> clienteService.sair(1L),
                NegocioException.class
        );

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void alterarValorMensal_valorValido_atualiza() {
        Cliente cliente = new Cliente("João", "12345678901", "j@e.com", BigDecimal.valueOf(1000));
        cliente.setId(1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any())).thenReturn(cliente);
        when(contaGraficaRepository.findByClienteIdAndTipo(1L, ContaGrafica.TipoConta.FILHOTE))
                .thenReturn(Optional.empty());

        ClienteResponse response = clienteService.alterarValorMensal(1L, BigDecimal.valueOf(5000));

        assertThat(cliente.getValorMensal()).isEqualByComparingTo("5000");
    }

    @Test
    void consultar_clienteInexistente_lancaNotFound() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        NegocioException ex = catchThrowableOfType(
                () -> clienteService.consultar(99L),
                NegocioException.class
        );

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
