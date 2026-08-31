package br.com.clube3barbas.api.rateio;

import br.com.clube3barbas.domain.rateio.RateioClubeService;
import br.com.clube3barbas.domain.rateio.ResultadoGeralRateio;
import br.com.clube3barbas.domain.rateio.ResultadoRateio;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fechamento real do rateio, calculado a partir de assinantes e atendimentos
 * persistidos. Diferente de RateioController (que so simula com valores enviados
 * pelo navegador), estes endpoints usam o RateioClubeService.
 */
@RestController
@RequestMapping(path = "/api/v1/rateios", produces = MediaType.APPLICATION_JSON_VALUE)
public class RateioClubeController {

    private final RateioClubeService rateioClubeService;

    public RateioClubeController(RateioClubeService rateioClubeService) {
        this.rateioClubeService = rateioClubeService;
    }

    @GetMapping("/assinantes/{assinanteId}")
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRATIVO', 'BARBEIRO')")
    public ResultadoRateio calcularParaAssinante(@PathVariable String assinanteId) {
        return rateioClubeService.calcularParaAssinante(assinanteId);
    }

    @GetMapping("/geral")
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRATIVO', 'BARBEIRO')")
    public ResultadoGeralRateio calcularGeral() {
        return rateioClubeService.calcularGeral();
    }
}
