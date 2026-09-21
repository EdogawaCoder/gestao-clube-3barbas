package br.com.clube3barbas.domain.assinante;

import java.util.List;

public interface HistoricoCicloRepository {

    List<HistoricoCiclo> listarPorAssinante(String assinanteId);

    HistoricoCiclo registrar(HistoricoCiclo historicoCiclo);
}
