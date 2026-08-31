package br.com.clube3barbas.domain.atendimento;

import java.util.List;
import java.util.Optional;

public interface AtendimentoRepository {

    List<Atendimento> listarPorAssinante(String assinanteId);

    List<Atendimento> listarTodos();

    Optional<Atendimento> buscarPorId(String id);

    Atendimento salvar(Atendimento atendimento);

    void remover(String id);
}
