package br.com.clube3barbas.api.assinante;

import br.com.clube3barbas.domain.assinante.Assinante;
import br.com.clube3barbas.domain.assinante.AssinanteService;
import br.com.clube3barbas.domain.assinante.HistoricoCiclo;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/assinantes", produces = MediaType.APPLICATION_JSON_VALUE)
public class AssinanteController {

    private final AssinanteService service;

    public AssinanteController(AssinanteService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRATIVO', 'BARBEIRO')")
    public List<Assinante> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRATIVO', 'BARBEIRO')")
    public Assinante buscar(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRATIVO', 'BARBEIRO')")
    @ResponseStatus(HttpStatus.CREATED)
    public Assinante cadastrar(@Valid @RequestBody NovoAssinanteRequest request) {
        return service.cadastrar(
                request.nome(),
                request.valorPlano(),
                request.percentualGerencia(),
                request.percentualBarbeiros()
        );
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRATIVO', 'BARBEIRO')")
    public Assinante atualizar(@PathVariable String id, @Valid @RequestBody AtualizarAssinanteRequest request) {
        return service.atualizar(
                id,
                request.nome(),
                request.valorPlano(),
                request.percentualGerencia(),
                request.percentualBarbeiros(),
                request.cicloInicio()
        );
    }

    @GetMapping("/{id}/ciclos")
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRATIVO', 'BARBEIRO')")
    public List<HistoricoCiclo> listarCiclosAnteriores(@PathVariable String id) {
        return service.listarCiclosAnteriores(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRATIVO', 'BARBEIRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        service.excluir(id);
    }
}
