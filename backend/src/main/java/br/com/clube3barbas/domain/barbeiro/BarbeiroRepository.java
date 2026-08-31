package br.com.clube3barbas.domain.barbeiro;

import java.util.List;
import java.util.Optional;

public interface BarbeiroRepository {

    List<Barbeiro> listarAtivos();

    Optional<Barbeiro> buscarPorId(String id);

    Barbeiro salvar(Barbeiro barbeiro);
}
