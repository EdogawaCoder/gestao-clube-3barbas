package br.com.clube3barbas.domain.assinante;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AssinanteService {

    private final AssinanteRepository repository;

    public AssinanteService(AssinanteRepository repository) {
        this.repository = repository;
    }

    public List<Assinante> listar() {
        return repository.listar();
    }

    public Assinante buscarPorId(String id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Assinante nao encontrado."));
    }

    public Assinante cadastrar(
            String nome,
            BigDecimal valorPlano,
            BigDecimal percentualGerencia,
            BigDecimal percentualBarbeiros
    ) {
        var assinante = new Assinante(
                UUID.randomUUID().toString(),
                nome,
                valorPlano,
                percentualGerencia,
                percentualBarbeiros,
                Instant.now()
        );
        return repository.salvar(assinante);
    }
}
