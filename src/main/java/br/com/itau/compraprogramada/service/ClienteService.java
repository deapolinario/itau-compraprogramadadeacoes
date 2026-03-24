package br.com.itau.compraprogramada.service;

import br.com.itau.compraprogramada.domain.cliente.Cliente;
import br.com.itau.compraprogramada.domain.custodia.ContaGrafica;
import br.com.itau.compraprogramada.domain.custodia.Custodia;
import br.com.itau.compraprogramada.dto.AdesaoRequest;
import br.com.itau.compraprogramada.dto.ClienteResponse;
import br.com.itau.compraprogramada.exception.NegocioException;
import br.com.itau.compraprogramada.repository.ClienteRepository;
import br.com.itau.compraprogramada.repository.ContaGraficaRepository;
import br.com.itau.compraprogramada.repository.CustodiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ContaGraficaRepository contaGraficaRepository;
    private final CustodiaRepository custodiaRepository;

    @Transactional
    public ClienteResponse aderir(AdesaoRequest request) {
        if (clienteRepository.existsByCpf(request.getCpf())) {
            throw new NegocioException("CPF já cadastrado no sistema", HttpStatus.CONFLICT);
        }

        Cliente cliente = new Cliente(
                request.getNome(),
                request.getCpf(),
                request.getEmail(),
                request.getValorMensal()
        );
        clienteRepository.save(cliente);

        String numeroConta = "FILHOTE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ContaGrafica conta = new ContaGrafica(cliente, numeroConta, ContaGrafica.TipoConta.FILHOTE);
        contaGraficaRepository.save(conta);

        return ClienteResponse.of(cliente, numeroConta);
    }

    @Transactional
    public void sair(Long clienteId) {
        Cliente cliente = buscarCliente(clienteId);
        if (!cliente.getAtivo()) {
            throw new NegocioException("Cliente já está inativo", HttpStatus.CONFLICT);
        }
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    @Transactional
    public ClienteResponse alterarValorMensal(Long clienteId, BigDecimal novoValor) {
        Cliente cliente = buscarCliente(clienteId);
        cliente.setValorMensal(novoValor);
        clienteRepository.save(cliente);

        String numeroConta = contaGraficaRepository
                .findByClienteIdAndTipo(clienteId, ContaGrafica.TipoConta.FILHOTE)
                .map(ContaGrafica::getNumeroConta)
                .orElse(null);

        return ClienteResponse.of(cliente, numeroConta);
    }

    @Transactional(readOnly = true)
    public ClienteResponse consultar(Long clienteId) {
        Cliente cliente = buscarCliente(clienteId);

        String numeroConta = contaGraficaRepository
                .findByClienteIdAndTipo(clienteId, ContaGrafica.TipoConta.FILHOTE)
                .map(ContaGrafica::getNumeroConta)
                .orElse(null);

        return ClienteResponse.of(cliente, numeroConta);
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new NegocioException("Cliente não encontrado", HttpStatus.NOT_FOUND));
    }
}
