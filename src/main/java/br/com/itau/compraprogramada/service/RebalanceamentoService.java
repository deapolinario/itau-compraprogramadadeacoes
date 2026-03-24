package br.com.itau.compraprogramada.service;

import br.com.itau.compraprogramada.cotahist.CotahistParser;
import br.com.itau.compraprogramada.domain.cesta.CestaRecomendacao;
import br.com.itau.compraprogramada.domain.cesta.ItemCesta;
import br.com.itau.compraprogramada.domain.cliente.Cliente;
import br.com.itau.compraprogramada.domain.custodia.ContaGrafica;
import br.com.itau.compraprogramada.domain.custodia.Custodia;
import br.com.itau.compraprogramada.domain.motor.HistoricoOperacao;
import br.com.itau.compraprogramada.domain.motor.HistoricoOperacao.TipoOperacao;
import br.com.itau.compraprogramada.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RebalanceamentoService {

    private final ClienteRepository clienteRepository;
    private final ContaGraficaRepository contaGraficaRepository;
    private final CustodiaRepository custodiaRepository;
    private final HistoricoOperacaoRepository historicoRepository;
    private final CotahistParser cotahistParser;
    private final FiscalService fiscalService;

    /**
     * Executa rebalanceamento para todos os clientes ativos ao mudar a cesta.
     * 9.1: Identifica ativos removidos, adicionados e com percentual alterado.
     */
    @Transactional
    public void executar(CestaRecomendacao cestaAntiga, CestaRecomendacao cestaNova) {
        List<Cliente> clientesAtivos = clienteRepository.findAllByAtivoTrue();
        if (clientesAtivos.isEmpty()) return;

        Map<String, BigDecimal> tickersAntigos = cestaAntiga == null ? Map.of() :
                cestaAntiga.getItens().stream()
                        .collect(Collectors.toMap(ItemCesta::getTicker, ItemCesta::getPercentual));

        Map<String, BigDecimal> tickersNovos = cestaNova.getItens().stream()
                .collect(Collectors.toMap(ItemCesta::getTicker, ItemCesta::getPercentual));

        Set<String> removidos = new HashSet<>(tickersAntigos.keySet());
        removidos.removeAll(tickersNovos.keySet());

        Set<String> adicionados = new HashSet<>(tickersNovos.keySet());
        adicionados.removeAll(tickersAntigos.keySet());

        Set<String> alterados = tickersNovos.keySet().stream()
                .filter(t -> tickersAntigos.containsKey(t) &&
                        tickersAntigos.get(t).compareTo(tickersNovos.get(t)) != 0)
                .collect(Collectors.toSet());

        // Busca cotações de todos os tickers envolvidos
        Set<String> todosTickers = new HashSet<>();
        todosTickers.addAll(removidos);
        todosTickers.addAll(adicionados);
        todosTickers.addAll(alterados);
        todosTickers.addAll(tickersNovos.keySet());

        if (todosTickers.isEmpty()) return;

        Map<String, BigDecimal> cotacoes = cotahistParser.buscarCotacoes(new ArrayList<>(todosTickers));

                List<Long> clienteIds = clientesAtivos.stream().map(Cliente::getId).toList();
                List<ContaGrafica> contasFilhote = contaGraficaRepository
                                .findAllByClienteInAndTipo(clienteIds, ContaGrafica.TipoConta.FILHOTE);
                Map<Long, ContaGrafica> contasPorCliente = contasFilhote.stream()
                                .collect(Collectors.toMap(c -> c.getCliente().getId(), c -> c));

                List<Long> contaIds = contasFilhote.stream().map(ContaGrafica::getId).toList();
                Map<String, Custodia> custodiasPorContaTicker = new HashMap<>();
                Map<Long, List<Custodia>> custodiasPorConta = new HashMap<>();

                if (!contaIds.isEmpty()) {
                        List<Custodia> custodias = custodiaRepository
                                        .findAllByContaInAndTickerIn(contaIds, new ArrayList<>(todosTickers));
                        custodias.forEach(c -> {
                                custodiasPorContaTicker.put(chaveCustodia(c.getConta().getId(), c.getTicker()), c);
                                custodiasPorConta.computeIfAbsent(c.getConta().getId(), k -> new ArrayList<>()).add(c);
                        });
                }

        for (Cliente cliente : clientesAtivos) {
            try {
                rebalancearCliente(cliente, removidos, adicionados, alterados,
                                                tickersNovos, cotacoes, contasPorCliente,
                                                custodiasPorContaTicker, custodiasPorConta);
            } catch (Exception e) {
                log.error("Erro ao rebalancear cliente {}: {}", cliente.getId(), e.getMessage(), e);
            }
        }
    }

    private void rebalancearCliente(Cliente cliente, Set<String> removidos, Set<String> adicionados,
                                                                         Set<String> alterados, Map<String, BigDecimal> tickersNovos,
                                                                         Map<String, BigDecimal> cotacoes,
                                                                         Map<Long, ContaGrafica> contasPorCliente,
                                                                         Map<String, Custodia> custodiasPorContaTicker,
                                                                         Map<Long, List<Custodia>> custodiasPorConta) {
                ContaGrafica contaFilhote = contasPorCliente.get(cliente.getId());
        if (contaFilhote == null) return;

        BigDecimal valorVendas = BigDecimal.ZERO;
        BigDecimal lucroLiquido = BigDecimal.ZERO;
        List<HistoricoOperacao> vendasDoMes = new ArrayList<>();

        // 9.2: Vender ativos removidos
        for (String ticker : removidos) {
                        Custodia pos = custodiasPorContaTicker.get(chaveCustodia(contaFilhote.getId(), ticker));
            if (pos == null || pos.getQuantidade() == 0) continue;

            BigDecimal cotacao = cotacoes.getOrDefault(ticker, BigDecimal.ZERO);
                        if (cotacao.compareTo(BigDecimal.ZERO) <= 0) {
                                log.warn("Cotação inválida para ticker {} no rebalanceamento de venda. Operação ignorada.", ticker);
                                continue;
                        }

            BigDecimal valorVenda = cotacao.multiply(BigDecimal.valueOf(pos.getQuantidade()));
            BigDecimal lucro = cotacao.subtract(pos.getPrecoMedio())
                    .multiply(BigDecimal.valueOf(pos.getQuantidade()));

            valorVendas = valorVendas.add(valorVenda);
            lucroLiquido = lucroLiquido.add(lucro);

            HistoricoOperacao op = new HistoricoOperacao(cliente, contaFilhote, ticker,
                    TipoOperacao.VENDA, pos.getQuantidade(), cotacao);
            historicoRepository.save(op);
            vendasDoMes.add(op);

            pos.setQuantidade(0L);
            custodiaRepository.save(pos);
        }

        // 9.4: Ajustar ativos com percentual alterado
                BigDecimal valorTotalCarteira = calcularValorTotal(contaFilhote.getId(), cotacoes, custodiasPorConta);

        for (String ticker : alterados) {
                        Custodia pos = custodiasPorContaTicker.get(chaveCustodia(contaFilhote.getId(), ticker));
            if (pos == null || pos.getQuantidade() == 0) continue;

            BigDecimal cotacao = cotacoes.getOrDefault(ticker, BigDecimal.ZERO);
                        if (cotacao.compareTo(BigDecimal.ZERO) <= 0) {
                                log.warn("Cotação inválida para ticker {} no rebalanceamento de ajuste. Operação ignorada.", ticker);
                                continue;
                        }

            BigDecimal percentualNovo = tickersNovos.get(ticker).divide(BigDecimal.valueOf(100), 4, RoundingMode.DOWN);
            BigDecimal valorAlvo = valorTotalCarteira.multiply(percentualNovo);
            BigDecimal valorAtual = cotacao.multiply(BigDecimal.valueOf(pos.getQuantidade()));

            if (valorAtual.compareTo(valorAlvo) > 0) {
                // Sobre-alocado: vender excesso
                BigDecimal excesso = valorAtual.subtract(valorAlvo);
                long qtdVender = excesso.divide(cotacao, 0, RoundingMode.DOWN).longValue();
                if (qtdVender > 0 && qtdVender <= pos.getQuantidade()) {
                    BigDecimal valorV = cotacao.multiply(BigDecimal.valueOf(qtdVender));
                    BigDecimal lucro = cotacao.subtract(pos.getPrecoMedio()).multiply(BigDecimal.valueOf(qtdVender));
                    valorVendas = valorVendas.add(valorV);
                    lucroLiquido = lucroLiquido.add(lucro);

                    HistoricoOperacao op = new HistoricoOperacao(cliente, contaFilhote, ticker,
                            TipoOperacao.VENDA, qtdVender, cotacao);
                    historicoRepository.save(op);
                    vendasDoMes.add(op);

                    pos.setQuantidade(pos.getQuantidade() - qtdVender);
                    custodiaRepository.save(pos);
                }
            }
        }

        // 9.3: Comprar ativos adicionados com valor obtido nas vendas
        if (valorVendas.compareTo(BigDecimal.ZERO) > 0 && !adicionados.isEmpty()) {
            BigDecimal somaPercentuaisNovos = adicionados.stream()
                    .map(tickersNovos::get)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                        if (somaPercentuaisNovos.compareTo(BigDecimal.ZERO) <= 0) {
                                log.warn("Soma dos percentuais dos ativos adicionados é inválida. Compras de rebalanceamento ignoradas.");
                        }

            for (String ticker : adicionados) {
                                if (somaPercentuaisNovos.compareTo(BigDecimal.ZERO) <= 0) {
                                        break;
                                }

                BigDecimal cotacao = cotacoes.getOrDefault(ticker, BigDecimal.ZERO);
                if (cotacao.compareTo(BigDecimal.ZERO) == 0) continue;

                BigDecimal fracaoDoValor = tickersNovos.get(ticker)
                        .divide(somaPercentuaisNovos, 6, RoundingMode.DOWN);
                BigDecimal valorCompra = valorVendas.multiply(fracaoDoValor);
                long qtdComprar = valorCompra.divide(cotacao, 0, RoundingMode.DOWN).longValue();
                if (qtdComprar == 0) continue;

                                String chave = chaveCustodia(contaFilhote.getId(), ticker);
                                Custodia pos = custodiasPorContaTicker.get(chave);
                                if (pos == null) {
                                        pos = new Custodia(contaFilhote, ticker);
                                        custodiasPorContaTicker.put(chave, pos);
                                        custodiasPorConta.computeIfAbsent(contaFilhote.getId(), k -> new ArrayList<>()).add(pos);
                                }

                pos.setPrecoMedio(calcularNovoPrecoMedio(pos, qtdComprar, cotacao));
                pos.setQuantidade(pos.getQuantidade() + qtdComprar);
                custodiaRepository.save(pos);

                historicoRepository.save(new HistoricoOperacao(cliente, contaFilhote, ticker,
                        TipoOperacao.COMPRA, qtdComprar, cotacao));
            }
        }

        // 8.5: Publica IR venda
        if (!vendasDoMes.isEmpty()) {
            fiscalService.calcularEPublicarIRVenda(cliente, vendasDoMes, lucroLiquido);
        }
    }

        private BigDecimal calcularValorTotal(Long contaId, Map<String, BigDecimal> cotacoes,
                                                                                  Map<Long, List<Custodia>> custodiasPorConta) {
                return custodiasPorConta.getOrDefault(contaId, List.of()).stream()
                .filter(c -> c.getQuantidade() > 0)
                .map(c -> cotacoes.getOrDefault(c.getTicker(), BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(c.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

        private String chaveCustodia(Long contaId, String ticker) {
                return contaId + ":" + ticker;
        }

    private BigDecimal calcularNovoPrecoMedio(Custodia pos, long qtdNova, BigDecimal preco) {
        if (pos.getQuantidade() == 0) return preco.setScale(6, RoundingMode.DOWN);
        BigDecimal valorAnt = pos.getPrecoMedio().multiply(BigDecimal.valueOf(pos.getQuantidade()));
        BigDecimal valorNovo = preco.multiply(BigDecimal.valueOf(qtdNova));
        long qtdTotal = pos.getQuantidade() + qtdNova;
        return valorAnt.add(valorNovo).divide(BigDecimal.valueOf(qtdTotal), 6, RoundingMode.DOWN);
    }
}
