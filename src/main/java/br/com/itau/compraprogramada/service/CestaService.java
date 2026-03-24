package br.com.itau.compraprogramada.service;

import br.com.itau.compraprogramada.domain.cesta.CestaAlteradaEvent;
import br.com.itau.compraprogramada.domain.cesta.CestaRecomendacao;
import br.com.itau.compraprogramada.domain.cesta.ItemCesta;
import br.com.itau.compraprogramada.dto.CestaRequest;
import br.com.itau.compraprogramada.dto.CestaResponse;
import br.com.itau.compraprogramada.exception.NegocioException;
import br.com.itau.compraprogramada.repository.CestaRecomendacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CestaService {

    private final CestaRecomendacaoRepository cestaRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CestaResponse criar(CestaRequest request) {
        validarCesta(request);

        CestaRecomendacao cestaAntiga = cestaRepository.findByAtivoTrue().orElse(null);

        CestaRecomendacao novaCesta = new CestaRecomendacao();
        request.getItens().forEach(item -> {
            ItemCesta itemCesta = new ItemCesta(novaCesta, item.getTicker().toUpperCase(), item.getPercentual());
            novaCesta.getItens().add(itemCesta);
        });
        cestaRepository.save(novaCesta);

        if (cestaAntiga != null) {
            cestaAntiga.desativar();
            cestaRepository.save(cestaAntiga);
            eventPublisher.publishEvent(new CestaAlteradaEvent(cestaAntiga, novaCesta));
        }

        return CestaResponse.of(novaCesta);
    }

    @Transactional(readOnly = true)
    public CestaResponse buscarAtiva() {
        CestaRecomendacao cesta = cestaRepository.findByAtivoTrue()
                .orElseThrow(() -> new NegocioException("Nenhuma cesta ativa encontrada", HttpStatus.NOT_FOUND));
        return CestaResponse.of(cesta);
    }

    private void validarCesta(CestaRequest request) {
        BigDecimal soma = request.getItens().stream()
                .map(CestaRequest.ItemCestaRequest::getPercentual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (soma.compareTo(new BigDecimal("100.00")) != 0) {
            throw new NegocioException(
                    "A soma dos percentuais deve ser exatamente 100%. Soma atual: " + soma,
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
