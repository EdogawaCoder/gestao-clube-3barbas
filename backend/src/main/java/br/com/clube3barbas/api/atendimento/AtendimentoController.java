package br.com.clube3barbas.api.atendimento;

import br.com.clube3barbas.domain.atendimento.Atendimento;
import br.com.clube3barbas.domain.atendimento.AtendimentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/assinantes/{assinanteId}/atendimentos", produces = MediaType.APPLICATION_JSON_VALUE)
public class AtendimentoController {

    private final AtendimentoService service;

    public AtendimentoController(AtendimentoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRATIVO', 'BARBEIRO')")
    public List<Atendimento> listar(@PathVariable String assinanteId) {
        return service.listarPorAssinante(assinanteId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRATIVO', 'BARBEIRO')")
    @ResponseStatus(HttpStatus.CREATED)
    public Atendimento registrar(
            @PathVariable String assinanteId,
            @Valid @RequestBody NovoAtendimentoRequest request,
            Authentication authentication
    ) {
        var gerente = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_GERENTE".equals(authority.getAuthority()));
        return service.registrar(assinanteId, request.barbeiroId(), request.dataHora(), gerente);
    }

    @DeleteMapping("/{atendimentoId}")
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRATIVO', 'BARBEIRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable String assinanteId, @PathVariable String atendimentoId) {
        service.remover(assinanteId, atendimentoId);
    }
}
