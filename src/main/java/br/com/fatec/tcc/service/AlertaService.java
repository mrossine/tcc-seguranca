package br.com.fatec.tcc.service;

import br.com.fatec.tcc.dto.AlertaRequestDTO;
import br.com.fatec.tcc.dto.AlertaResponseDTO;
import br.com.fatec.tcc.model.Alerta;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.repository.AlertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final UsuarioService usuarioService;

    @Transactional
    public AlertaResponseDTO criarAlerta(AlertaRequestDTO request, String email) {
        Usuario usuario = usuarioService.findUserByUsername(email);

        Alerta alerta = new Alerta();
        alerta.setTitulo(request.titulo());
        alerta.setDescricao(request.descricao());
        alerta.setTipo(request.tipo());
        alerta.setLocalizacao(request.localizacao());
        alerta.setLatitude(request.latitude());
        alerta.setLongitude(request.longitude());
        alerta.setDataHora(LocalDateTime.now());
        alerta.setUsuario(usuario);
        alerta.setStatus(Alerta.StatusAlerta.ATIVO);

        Alerta saved = alertaRepository.save(alerta);
        return convertToResponseDTO(saved, usuario);
    }

    /**
     * Lista alertas ativos dos últimos 14 dias (visibilidade pública).
     * Alertas mais antigos permanecem no banco para estatísticas.
     */
    public List<AlertaResponseDTO> listarAlertasAtivos(String emailLogado) {
        Usuario usuarioLogado = usuarioService.findUserByUsername(emailLogado);
        LocalDateTime limite = LocalDateTime.now().minusDays(14);
        List<Alerta> alertas = alertaRepository
                .findByStatusAndDataCriacaoAfterOrderByDataCriacaoDesc(Alerta.StatusAlerta.ATIVO, limite);
        return alertas.stream()
                .map(alerta -> convertToResponseDTO(alerta, usuarioLogado))
                .collect(Collectors.toList());
    }

    @Transactional
    public void confirmarAlerta(Long id) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta não encontrado"));
        alerta.setConfirmacoes(alerta.getConfirmacoes() + 1);
        alertaRepository.save(alerta);
    }

    @Transactional
    public void denunciarAlerta(Long id) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta não encontrado"));
        alerta.setDenuncias(alerta.getDenuncias() + 1);
        if (alerta.getDenuncias() >= 5) {
            alerta.setStatus(Alerta.StatusAlerta.DENUNCIADO);
        }
        alertaRepository.save(alerta);
    }

    @Transactional
    public void removerAlerta(Long id, String email) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta não encontrado"));
        Usuario usuario = usuarioService.findUserByUsername(email);
        
        // Permissão: autor do alerta, moderador ou admin
        if (!alerta.getUsuario().getId().equals(usuario.getId()) &&
            usuario.getRole() != Usuario.Role.MODERATOR &&
            usuario.getRole() != Usuario.Role.ADMIN) {
            throw new RuntimeException("Sem permissão para remover este alerta");
        }
        alertaRepository.delete(alerta);
    }

    public List<AlertaResponseDTO> listarAlertasPorUsuario(Usuario usuario) {
        return alertaRepository.findByUsuarioOrderByDataCriacaoDesc(usuario)
                .stream()
                .map(alerta -> convertToResponseDTO(alerta, usuario))
                .collect(Collectors.toList());
    }

    private AlertaResponseDTO convertToResponseDTO(Alerta alerta, Usuario usuarioLogado) {
        boolean podeExcluir = false;
        boolean meuAlerta = false;
        if (usuarioLogado != null) {
            meuAlerta = alerta.getUsuario().getId().equals(usuarioLogado.getId());
            podeExcluir = meuAlerta ||
                          usuarioLogado.getRole() == Usuario.Role.ADMIN ||
                          usuarioLogado.getRole() == Usuario.Role.MODERATOR;
        }
        return new AlertaResponseDTO(
            alerta.getId(),
            alerta.getTitulo(),
            alerta.getDescricao(),
            alerta.getTipo(),
            alerta.getLocalizacao(),
            alerta.getLatitude(),
            alerta.getLongitude(),
            alerta.getDataHora(),
            alerta.getStatus(),
            alerta.getUsuario().getNomeCompleto(),
            alerta.getConfirmacoes(),
            alerta.getDenuncias(),
            alerta.getDataCriacao(),
            podeExcluir,
            meuAlerta
        );
    }
}