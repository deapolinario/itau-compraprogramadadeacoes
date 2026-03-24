package br.com.itau.compraprogramada.controller;

import br.com.itau.compraprogramada.dto.AdesaoRequest;
import br.com.itau.compraprogramada.dto.AlterarValorMensalRequest;
import br.com.itau.compraprogramada.dto.ClienteResponse;
import br.com.itau.compraprogramada.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gestão de clientes do produto de compra programada")
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    @Operation(summary = "Aderir ao produto")
    public ResponseEntity<ClienteResponse> aderir(@RequestBody @Valid AdesaoRequest request) {
        ClienteResponse response = clienteService.aderir(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Sair do produto")
    public ResponseEntity<Void> sair(@PathVariable Long id) {
        clienteService.sair(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/valor-mensal")
    @Operation(summary = "Alterar valor mensal de aporte")
    public ResponseEntity<ClienteResponse> alterarValorMensal(
            @PathVariable Long id,
            @RequestBody @Valid AlterarValorMensalRequest request) {
        return ResponseEntity.ok(clienteService.alterarValorMensal(id, request.getValorMensal()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar dados do cliente")
    public ResponseEntity<ClienteResponse> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.consultar(id));
    }
}
