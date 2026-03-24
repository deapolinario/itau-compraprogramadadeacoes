package br.com.itau.compraprogramada.service;

import br.com.itau.compraprogramada.domain.cesta.CestaAlteradaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RebalanceamentoListener {

    private final RebalanceamentoService rebalanceamentoService;

    @Async
    @EventListener
    public void onCestaAlterada(CestaAlteradaEvent event) {
        log.info("Cesta alterada detectada. Iniciando rebalanceamento assíncrono.");
        rebalanceamentoService.executar(event.cestaAntiga(), event.cestaNova());
        log.info("Rebalanceamento concluído.");
    }
}
