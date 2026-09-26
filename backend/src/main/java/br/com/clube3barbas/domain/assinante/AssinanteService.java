package br.com.clube3barbas.domain.assinante;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AssinanteService {

    private final AssinanteRepository repository;
    private final HistoricoCicloRepository historicoCicloRepository;

    public AssinanteService(AssinanteRepository repository, HistoricoCicloRepository historicoCicloRepository) {
        this.repository = repository;
        this.historicoCicloRepository = historicoCicloRepository;
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
     * do ciclo vigente). O ID e a data de cadastro original nunca mudam. Se o inicio
     * do ciclo mudou, o ciclo anterior e' arquivado no historico antes de sobrescrever
     * -- e' assim que a linha do tempo de ciclos do assinante e' construida.
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
        if (!existente.cicloInicio().equals(cicloInicio)) {
            historicoCicloRepository.registrar(new HistoricoCiclo(
                    UUID.randomUUID().toString(),
                    id,
                    existente.cicloInicio(),
                    Instant.now()
            ));
        }
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

    /** Lista os ciclos anteriores do assinante (o ciclo atual fica em Assinante.cicloInicio). */
    public List<HistoricoCiclo> listarCiclosAnteriores(String assinanteId) {
        buscarPorId(assinanteId);
        return historicoCicloRepository.listarPorAssinante(assinanteId);
    }

    /**
     * Remove um ciclo encerrado da linha do tempo -- pensado para desfazer registros
     * criados por engano (ex.: uma data de inicio digitada errada e depois corrigida).
     * Nao mexe no ciclo vigente nem nos atendimentos.
     */
    public void removerCicloAnterior(String assinanteId, String cicloId) {
        var existe = listarCiclosAnteriores(assinanteId).stream()
                .anyMatch(ciclo -> ciclo.id().equals(cicloId));
        if (!existe) {
            throw new IllegalArgumentException("Ciclo nao encontrado para este assinante.");
        }
        historicoCicloRepository.remover(cicloId);
    }

    /** Reinicia o ciclo vigente, arquivando o início atual antes de começar um novo ciclo. */
    public Assinante reiniciarCiclo(String id) {
        var existente = buscarPorId(id);
        var novoInicio = Instant.now();

        historicoCicloRepository.registrar(new HistoricoCiclo(
                UUID.randomUUID().toString(),
                id,
                existente.cicloInicio(),
                Instant.now()
        ));

        var atualizado = new Assinante(
                existente.id(),
                existente.nome(),
                existente.valorPlano(),
                existente.percentualGerencia(),
                existente.percentualBarbeiros(),
                novoInicio,
                existente.criadoEm()
        );
        return repository.salvar(atualizado);
    }

    /**
     * Exclui o assinante definitivamente -- pensado para corrigir cadastros feitos
     * por engano (ex.: duplicados). O historico de atendimentos e de ciclos dele
     * nao e apagado junto, apenas deixa de ter um assinante vivo associado.
     */
    public void excluir(String id) {
        buscarPorId(id);
        repository.remover(id);
    }
}
