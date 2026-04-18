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
		Usuario usuario = usuarioService.findUserByUsername(email); // ANTES era loadUserByUsername

		Alerta alerta = new Alerta();
		alerta.setTitulo(request.getTitulo());
		alerta.setDescricao(request.getDescricao());
		alerta.setTipo(request.getTipo());
		alerta.setLocalizacao(request.getLocalizacao());
		alerta.setLatitude(request.getLatitude());
		alerta.setLongitude(request.getLongitude());
		alerta.setDataHora(request.getDataHora() != null ? request.getDataHora() : LocalDateTime.now());
		alerta.setUsuario(usuario);
		alerta.setStatus(Alerta.StatusAlerta.ATIVO);

		Alerta saved = alertaRepository.save(alerta);
		return convertToResponseDTO(saved);
	}

	public List<AlertaResponseDTO> listarAlertasAtivos() {
		return alertaRepository.findByStatusOrderByDataCriacaoDesc(Alerta.StatusAlerta.ATIVO).stream()
				.map(this::convertToResponseDTO).collect(Collectors.toList());
	}

	@Transactional
	public void confirmarAlerta(Long id) {
		Alerta alerta = alertaRepository.findById(id).orElseThrow(() -> new RuntimeException("Alerta não encontrado"));
		alerta.setConfirmacoes(alerta.getConfirmacoes() + 1);
		alertaRepository.save(alerta);
	}

	@Transactional
	public void denunciarAlerta(Long id) {
		Alerta alerta = alertaRepository.findById(id).orElseThrow(() -> new RuntimeException("Alerta não encontrado"));
		alerta.setDenuncias(alerta.getDenuncias() + 1);

		// Se tiver mais de 5 denúncias, marcar como denunciado
		if (alerta.getDenuncias() >= 5) {
			alerta.setStatus(Alerta.StatusAlerta.DENUNCIADO);
		}

		alertaRepository.save(alerta);
	}

	@Transactional
	public void removerAlerta(Long id, String email) {
		Alerta alerta = alertaRepository.findById(id).orElseThrow(() -> new RuntimeException("Alerta não encontrado"));

		Usuario usuario = usuarioService.findUserByUsername(email);

		// Apenas o autor ou moderador/admin pode remover
		if (!alerta.getUsuario().getId().equals(usuario.getId()) && usuario.getRole() != Usuario.Role.MODERATOR
				&& usuario.getRole() != Usuario.Role.ADMIN) {
			throw new RuntimeException("Sem permissão para remover este alerta");
		}

		alertaRepository.delete(alerta);
	}

	private AlertaResponseDTO convertToResponseDTO(Alerta alerta) {
		AlertaResponseDTO dto = new AlertaResponseDTO();
		dto.setId(alerta.getId());
		dto.setTitulo(alerta.getTitulo());
		dto.setDescricao(alerta.getDescricao());
		dto.setTipo(alerta.getTipo());
		dto.setLocalizacao(alerta.getLocalizacao());
		dto.setLatitude(alerta.getLatitude());
		dto.setLongitude(alerta.getLongitude());
		dto.setDataHora(alerta.getDataHora());
		dto.setStatus(alerta.getStatus());
		dto.setNomeUsuario(alerta.getUsuario().getNomeCompleto());
		dto.setConfirmacoes(alerta.getConfirmacoes());
		dto.setDenuncias(alerta.getDenuncias());
		dto.setDataCriacao(alerta.getDataCriacao());
		return dto;
	}
	
	public List<AlertaResponseDTO> listarAlertasPorUsuario(Usuario usuario) {
	    return alertaRepository.findByUsuarioOrderByDataCriacaoDesc(usuario)
	            .stream().map(this::convertToResponseDTO).collect(Collectors.toList());
	}
}
