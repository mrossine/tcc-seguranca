package br.com.fatec.tcc.service;

import br.com.fatec.tcc.model.ModeloCustom;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.repository.ModeloCustomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bean auxiliar que persiste ModeloCustom em transação própria (REQUIRES_NEW),
 * garantindo que o registro não seja revertido pelo rollback da transação chamadora.
 */
@Service
@RequiredArgsConstructor
public class ModeloCustomPersister {

    private final ModeloCustomRepository modeloCustomRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void salvar(Usuario solicitante, String marca, String modelo) {
        ModeloCustom custom = new ModeloCustom();
        custom.setSolicitante(solicitante);
        custom.setMarca(marca);
        custom.setModelo(modelo);
        modeloCustomRepository.save(custom);
    }
}
