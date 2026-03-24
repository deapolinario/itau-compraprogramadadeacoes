package br.com.itau.compraprogramada.controller;

import br.com.itau.compraprogramada.dto.CestaRequest;
import br.com.itau.compraprogramada.dto.CestaResponse;
import br.com.itau.compraprogramada.service.CestaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cestas")
@RequiredArgsConstructor
@Tag(name = "Cesta", description = "Gestão da cesta de recomendação Top Five")
public class CestaController {

    private final CestaService cestaService;

    @PostMapping
    @Operation(summary = "Criar nova cesta de recomendação")
    public ResponseEntity<CestaResponse> criar(@RequestBody @Valid CestaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cestaService.criar(request));
    }

    @GetMapping("/ativa")
    @Operation(summary = "Buscar cesta ativa")
    public ResponseEntity<CestaResponse> buscarAtiva() {
        return ResponseEntity.ok(cestaService.buscarAtiva());
    }
}
