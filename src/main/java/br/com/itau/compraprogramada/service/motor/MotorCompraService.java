package br.com.itau.compraprogramada.service.motor;

import br.com.itau.compraprogramada.cotahist.CotahistParser;
import br.com.itau.compraprogramada.domain.cesta.CestaRecomendacao;
import br.com.itau.compraprogramada.domain.cesta.ItemCesta;
import br.com.itau.compraprogramada.domain.cliente.Cliente;
import br.com.itau.compraprogramada.domain.custodia.ContaGrafica;
import br.com.itau.compraprogramada.domain.custodia.Custodia;
import br.com.itau.compraprogramada.domain.motor.ExecucaoMotor;
import br.com.itau.compraprogramada.domain.motor.ExecucaoMotor.StatusExecucao;
import br.com.itau.compraprogramada.domain.motor.HistoricoOperacao;
import br.com.itau.compraprogramada.domain.motor.HistoricoOperacao.TipoOperacao;
import br.com.itau.compraprogramada.exception.NegocioException;
import br.com.itau.compraprogramada.repository.*;
import br.com.itau.compraprogramada.service.FiscalService;
import br.com.itau.compraprogramada.service.PrecoMedioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MotorCompraService {

    private final ExecucaoMotorRepository execucaoMotorRepository;
    private final ClienteRepository clienteRepository;
    private final CestaRecomendacaoRepository cestaRepository;
    private final ContaGraficaRepository contaGraficaRepository;
    private final CustodiaRepository custodiaRepository;
    private final HistoricoOperacaoRepository historicoRepository;
    private final CotahistParser cotahistParser;
    private final PrecoMedioService precoMedioService;
    private final FiscalService fiscalService;

    @Transactional
    public void executar(LocalDate dataReferencia) {
        // 6.1 Idempotência
        if (execucaoMotorRepository.existsByDataReferenciaAndStatus(dataReferencia, StatusExecucao.CONCLUIDO)) {
            log.info("Motor de compra já executado com sucesso para {}. Ignorando.", dataReferencia);
            return;
        }

        ExecucaoMotor execucao = execucaoMotorRepository.findByDataReferencia(dataReferencia)
                .orElse(new ExecucaoMotor(dataReferencia));
        execucao.setStatus(StatusExecucao.EM_EXECUCAO);
        execucao.setIniciadoEm(LocalDateTime.now());
        execucaoMotorRepository.save(execucao);

        try {
            executarCompras(dataReferencia);
            execucao.setStatus(StatusExecucao.CONCLUIDO);
            execucao.setConcluidoEm(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Erro na execução do motor de compra para {}: {}", dataReferencia, e.getMessage(), e);
            execucao.setStatus(StatusExecucao.ERRO);
            execucao.setMensagemErro(e.getMessage());
        } finally {
            execucaoMotorRepository.save(execucao);
        }
    }

    private void executarCompras(LocalDate dataReferencia) {
        // 6.2 Agrupa clientes ativos
        List<Cliente> clientesAtivos = clienteRepository.findAllByAtivoTrue();
        if (clientesAtivos.isEmpty()) {
            log.info("Nenhum cliente ativo. Motor encerrado.");
            return;
        }

        BigDecimal totalConsolidado = clientesAtivos.stream()
                .map(c -> c.getValorMensal().divide(BigDecimal.valueOf(3), 2, RoundingMode.DOWN))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalConsolidado.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Total consolidado inválido para {}. Encerrando execução.", dataReferencia);
            return;
        }

        log.info("Total consolidado para {}: R$ {}", dataReferencia, totalConsolidado);

        // Cesta ativa
        CestaRecomendacao cesta = cestaRepository.findByAtivoTrue()
                .orElseThrow(() -> new NegocioException("Nenhuma cesta ativa encontrada", HttpStatus.INTERNAL_SERVER_ERROR));

        // Cotações
        List<String> tickers = cesta.getItens().stream().map(ItemCesta::getTicker).toList();
        Map<String, BigDecimal> cotacoes = cotahistParser.buscarCotacoes(tickers);

        // Conta master
        ContaGrafica contaMaster = contaGraficaRepository.findByTipo(ContaGrafica.TipoConta.MASTER)
                .orElseThrow(() -> new NegocioException("Conta master não encontrada", HttpStatus.INTERNAL_SERVER_ERROR));

        // 6.3, 6.4, 6.5: Calcula e registra compras na master
        for (ItemCesta item : cesta.getItens()) {
            String ticker = item.getTicker();
            BigDecimal cotacao = cotacoes.get(ticker);
            if (cotacao == null || cotacao.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Cotação inválida para ticker {}. Compra ignorada.", ticker);
                continue;
            }

            BigDecimal percentual = item.getPercentual().divide(BigDecimal.valueOf(100), 4, RoundingMode.DOWN);
            BigDecimal valorAlvo = totalConsolidado.multiply(percentual);

            long qtdBruta = valorAlvo.divide(cotacao, 0, RoundingMode.DOWN).longValue();

            Custodia saldoMaster = custodiaRepository.findByContaIdAndTicker(contaMaster.getId(), ticker)
                    .orElse(new Custodia(contaMaster, ticker));

            long qtdComprar = Math.max(0, qtdBruta - saldoMaster.getQuantidade());

            if (qtdComprar > 0) {
                // 6.4 Separa lote padrão e fracionário
                long lotePadrao = (qtdComprar / 100) * 100;
                long fracionario = qtdComprar % 100;

                if (lotePadrao > 0) {
                    registrarCompra(contaMaster, null, ticker, lotePadrao, cotacao);
                }
                if (fracionario > 0) {
                    registrarCompra(contaMaster, null, ticker + "F", fracionario, cotacao);
                }

                // Atualiza saldo master
                saldoMaster.setQuantidade(saldoMaster.getQuantidade() + qtdComprar);
                custodiaRepository.save(saldoMaster);
            }
        }

        // 6.6, 6.7: Distribui para filhotes
        distribuirParaFilhotes(clientesAtivos, cesta, cotacoes, contaMaster, totalConsolidado);
    }

    private void distribuirParaFilhotes(List<Cliente> clientes, CestaRecomendacao cesta,
                                         Map<String, BigDecimal> cotacoes,
                                         ContaGrafica contaMaster, BigDecimal totalConsolidado) {
        // OTIMIZAÇÃO: Buscar todas as contas filhotes de uma vez ao invés de N queries
        List<Long> clienteIds = clientes.stream().map(Cliente::getId).toList();
        List<ContaGrafica> contasFilhote = contaGraficaRepository
                .findAllByClienteInAndTipo(clienteIds, ContaGrafica.TipoConta.FILHOTE);
        
        Map<Long, ContaGrafica> contasMap = contasFilhote.stream()
                .collect(Collectors.toMap(c -> c.getCliente().getId(), c -> c));

        // OTIMIZAÇÃO: Buscar todas as custódias necessárias de uma vez
        List<String> tickers = cesta.getItens().stream().map(ItemCesta::getTicker).toList();
        List<Long> contaIds = new ArrayList<>(contasMap.values().stream()
                .map(ContaGrafica::getId).toList());
        contaIds.add(contaMaster.getId());
        
        List<Custodia> custodias = custodiaRepository.findAllByContaInAndTickerIn(contaIds, tickers);
        Map<String, Custodia> custodiaMap = custodias.stream()
                .collect(Collectors.toMap(
                        c -> c.getConta().getId() + ":" + c.getTicker(),
                        c -> c,
                        (existing, replacement) -> {
                            log.warn("Custódia duplicada detectada para conta+ticker '{}:{}'. Mantendo registro existente.",
                                    existing.getConta().getId(), existing.getTicker());
                            return existing;
                        }
                ));

        for (ItemCesta item : cesta.getItens()) {
            String ticker = item.getTicker();
            BigDecimal cotacao = cotacoes.get(ticker);
            if (cotacao == null || cotacao.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Cotação inválida para ticker {}. Distribuição ignorada.", ticker);
                continue;
            }

            Custodia saldoMaster = custodiaMap.getOrDefault(contaMaster.getId() + ":" + ticker,
                    new Custodia(contaMaster, ticker));

            long totalDisponivel = saldoMaster.getQuantidade();
            if (totalDisponivel == 0) continue;

            long totalDistribuido = 0;

            for (Cliente cliente : clientes) {
                BigDecimal parcela = cliente.getValorMensal().divide(BigDecimal.valueOf(3), 2, RoundingMode.DOWN);
                BigDecimal proporcao = parcela.divide(totalConsolidado, 6, RoundingMode.DOWN);
                long qtdCliente = BigDecimal.valueOf(totalDisponivel)
                        .multiply(proporcao)
                        .setScale(0, RoundingMode.DOWN)
                        .longValue();

                if (qtdCliente == 0) continue;

                ContaGrafica contaFilhote = contasMap.get(cliente.getId());
                if (contaFilhote == null) {
                    log.warn("Conta filhote não encontrada para cliente {}", cliente.getId());
                    continue;
                }

                String custodiaKey = contaFilhote.getId() + ":" + ticker;
                Custodia custodiaFilhote = custodiaMap.getOrDefault(custodiaKey,
                        new Custodia(contaFilhote, ticker));

                // Atualiza PM antes de alterar quantidade
                BigDecimal novoPrecoMedio = precoMedioService.calcular(custodiaFilhote, qtdCliente, cotacao);
                custodiaFilhote.setQuantidade(custodiaFilhote.getQuantidade() + qtdCliente);
                custodiaFilhote.setPrecoMedio(novoPrecoMedio);
                custodiaRepository.save(custodiaFilhote);

                // Registra no histórico
                registrarCompra(contaFilhote, cliente, ticker, qtdCliente, cotacao);

                // Publica IR dedo-duro
                fiscalService.publicarIRDedoDuro(cliente, ticker, qtdCliente, cotacao);

                totalDistribuido += qtdCliente;
            }

            // Atualiza residuo na master
            long residuo = totalDisponivel - totalDistribuido;
            saldoMaster.setQuantidade(residuo);
            custodiaRepository.save(saldoMaster);

            log.info("Ticker {}: {} distribuídas, {} residuo na master", ticker, totalDistribuido, residuo);
        }
    }

    private void registrarCompra(ContaGrafica conta, Cliente cliente, String ticker,
                                  long quantidade, BigDecimal preco) {
        HistoricoOperacao op = new HistoricoOperacao(cliente, conta, ticker, TipoOperacao.COMPRA, quantidade, preco);
        historicoRepository.save(op);
    }
}
