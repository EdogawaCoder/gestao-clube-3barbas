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
        var agora = Instant.now();
        var assinante = new Assinante(
                UUID.randomUUID().toString(),
                nome,
                valorPlano,
                percentualGerencia,
                percentualBarbeiros,
                agora,
                agora
        );
        return repository.salvar(assinante);
    }

    /**
     * Atualiza os dados editaveis do assinante (nome, plano, percentuais e o inicio
     * do ciclo vigente). O ID e a data de cadastro original nunca mudam.
     */
    public Assinante atualizar(
            String id,
            String nome,
            BigDecimal valorPlano,
            BigDecimal percentualGerencia,
            BigDecimal percentualBarbeiros,
            Instant cicloInicio
    ) {
        var existente = buscarPorId(id);
        var atualizado = new Assinante(
                existente.id(),
                nome,
                valorPlano,
                percentualGerencia,
                percentualBarbeiros,
                cicloInicio,
                existente.criadoEm()
        );
        return repository.salvar(atualizado);
    }
}
