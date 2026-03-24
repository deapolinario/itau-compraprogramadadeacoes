package br.com.itau.compraprogramada.domain.cesta;

public record CestaAlteradaEvent(CestaRecomendacao cestaAntiga, CestaRecomendacao cestaNova) {
}
