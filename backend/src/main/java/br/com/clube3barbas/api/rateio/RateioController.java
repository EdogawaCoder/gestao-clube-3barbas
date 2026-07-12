package br.com.clube3barbas.api.rateio;

import br.com.clube3barbas.domain.rateio.AtendimentoRateio;
import br.com.clube3barbas.domain.rateio.RateioService;
import br.com.clube3barbas.domain.rateio.ResultadoRateio;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/rateios", produces = MediaType.APPLICATION_JSON_VALUE)
public class RateioController {

    private final RateioService rateioService;

    public RateioController(RateioService rateioService) {
        this.rateioService = rateioService;
    }

    @PostMapping(path = "/simular", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRATIVO', 'BARBEIRO')")
    public ResultadoRateio simular(@Valid @RequestBody SimulacaoRateioRequest request) {
        var atendimentos = request.atendimentos().stream()
                .map(item -> new AtendimentoRateio(item.barbeiroId(), item.barbeiroNome()))
                .toList();

        return rateioService.calcular(
                request.valorPlano(),
                request.percentualGerencia(),
                request.percentualBarbeiros(),
                atendimentos
        );
    }
}

