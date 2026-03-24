package br.com.itau.compraprogramada.controller;

import br.com.itau.compraprogramada.dto.CarteiraPosicaoResponse;
import br.com.itau.compraprogramada.service.RentabilidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Rentabilidade", description = "Consulta de carteira e rentabilidade")
public class RentabilidadeController {

    private final RentabilidadeService rentabilidadeService;

    @GetMapping("/{id}/carteira")
    @Operation(summary = "Consultar carteira e rentabilidade do cliente")
    public ResponseEntity<CarteiraPosicaoResponse> consultarCarteira(@PathVariable Long id) {
        return ResponseEntity.ok(rentabilidadeService.consultar(id));
    }
}
