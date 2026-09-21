package br.com.clube3barbas.domain.assinante;

import java.util.List;
import java.util.Optional;

public interface AssinanteRepository {

    List<Assinante> listar();

    Optional<Assinante> buscarPorId(String id);

    Assinante salvar(Assinante assinante);

    void remover(String id);
}
