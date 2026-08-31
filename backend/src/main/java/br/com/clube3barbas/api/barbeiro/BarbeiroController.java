package br.com.clube3barbas.api.barbeiro;

import br.com.clube3barbas.domain.barbeiro.Barbeiro;
import br.com.clube3barbas.domain.barbeiro.BarbeiroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/barbeiros", produces = MediaType.APPLICATION_JSON_VALUE)
public class BarbeiroController {

    private final BarbeiroService service;

    public BarbeiroController(BarbeiroService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRATIVO', 'BARBEIRO')")
    public List<Barbeiro> listar() {
        return service.listarAtivos();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('GERENTE')")
    @ResponseStatus(HttpStatus.CREATED)
    public Barbeiro cadastrar(@Valid @RequestBody NovoBarbeiroRequest request) {
        return service.cadastrar(request.nome());
    }
}
