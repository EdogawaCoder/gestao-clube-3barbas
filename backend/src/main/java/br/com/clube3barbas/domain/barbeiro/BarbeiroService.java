package br.com.clube3barbas.domain.barbeiro;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BarbeiroService {

    private final BarbeiroRepository repository;

    public BarbeiroService(BarbeiroRepository repository) {
        this.repository = repository;
    }

    public List<Barbeiro> listarAtivos() {
        return repository.listarAtivos();
    }

    public Barbeiro cadastrar(String nome) {
        var barbeiro = new Barbeiro(UUID.randomUUID().toString(), nome, true);
        return repository.salvar(barbeiro);
    }
}
