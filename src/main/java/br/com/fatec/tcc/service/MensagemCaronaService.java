package br.com.fatec.tcc.service;

import br.com.fatec.tcc.dto.MensagemCaronaDTO;
import br.com.fatec.tcc.model.Carona;
import br.com.fatec.tcc.model.MensagemCarona;
import br.com.fatec.tcc.model.ParticipacaoCarona;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.repository.CaronaRepository;
import br.com.fatec.tcc.repository.MensagemCaronaRepository;
import br.com.fatec.tcc.repository.ParticipacaoCaronaRepository;
import br.com.fatec.tcc.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de chat interno das caronas.
 * Apenas o motorista e passageiros com status CONFIRMADO podem trocar mensagens.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MensagemCaronaService {

    private final MensagemCaronaRepository mensagemRepository;
    private final CaronaRepository caronaRepository;
    private final ParticipacaoCaronaRepository participacaoRepository;
    private final UsuarioRepository usuarioRepository;

    /** Verifica se o usuário é motorista ou passageiro confirmado da carona. */
    private boolean temPermissao(Carona carona, Usuario usuario) {
        if (carona.getMotorista().getId().equals(usuario.getId())) return true;
        return participacaoRepository.findByCaronaAndPassageiro(carona, usuario)
                .map(p -> p.getStatus() == ParticipacaoCarona.StatusParticipacao.CONFIRMADA)
                .orElse(false);
    }

    private Usuario resolverUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + email));
    }

    /**
     * Persiste uma nova mensagem e retorna o DTO.
     * Lança AccessDeniedException se o remetente não for participante confirmado.
     */
    @Transactional
    public MensagemCaronaDTO enviarMensagem(Long caronaId, String emailRemetente, String conteudo) {
        if (conteudo == null || conteudo.isBlank()) {
            throw new IllegalArgumentException("Conteúdo da mensagem não pode ser vazio");
        }
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new EntityNotFoundException("Carona não encontrada"));
        Usuario remetente = resolverUsuario(emailRemetente);
        if (!temPermissao(carona, remetente)) {
            throw new AccessDeniedException("Apenas o motorista e passageiros confirmados podem enviar mensagens");
        }
        MensagemCarona msg = new MensagemCarona();
        msg.setCarona(carona);
        msg.setRemetente(remetente);
        msg.setConteudo(conteudo.trim());
        msg.setLido(false);
        MensagemCarona saved = mensagemRepository.save(msg);
        log.info("Chat — carona={} remetente={}", caronaId, emailRemetente);
        return MensagemCaronaDTO.fromEntity(saved);
    }

    /**
     * Persiste uma mensagem automática do sistema (ex.: avisos de entrada na carona).
     * O remetente é apenas uma referência (não há checagem de permissão).
     */
    @Transactional
    public void enviarMensagemSistema(Carona carona, Usuario referencia, String conteudo) {
        MensagemCarona msg = new MensagemCarona();
        msg.setCarona(carona);
        msg.setRemetente(referencia);
        msg.setConteudo(conteudo);
        msg.setLido(false);
        msg.setSistema(true);
        mensagemRepository.save(msg);
    }

    /**
     * Lista todas as mensagens da carona em ordem cronológica.
     * Marca como lidas as mensagens não lidas de outros remetentes.
     */
    @Transactional
    public List<MensagemCaronaDTO> listarMensagens(Long caronaId, String emailUsuario) {
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new EntityNotFoundException("Carona não encontrada"));
        Usuario usuarioLogado = resolverUsuario(emailUsuario);
        if (!temPermissao(carona, usuarioLogado)) {
            throw new AccessDeniedException("Sem permissão para ver as mensagens desta carona");
        }
        List<MensagemCarona> naoLidas = mensagemRepository
                .findByCaronaIdAndLidoFalseAndRemetenteIdNot(caronaId, usuarioLogado.getId());
        naoLidas.forEach(m -> m.setLido(true));
        if (!naoLidas.isEmpty()) mensagemRepository.saveAll(naoLidas);

        return mensagemRepository.findByCaronaIdOrderByDataCriacaoAsc(caronaId)
                .stream()
                .map(MensagemCaronaDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /** Conta mensagens não lidas enviadas por outros participantes. */
    @Transactional(readOnly = true)
    public long contarNaoLidos(Long caronaId, Usuario usuario) {
        return mensagemRepository.countByCaronaIdAndLidoFalseAndRemetenteIdNot(caronaId, usuario.getId());
    }
}
