package br.com.fatec.tcc.service;

import br.com.fatec.tcc.dto.CaronaRequestDTO;
import br.com.fatec.tcc.dto.CaronaResponseDTO;
import br.com.fatec.tcc.model.Carona;
import br.com.fatec.tcc.model.ParticipacaoCarona;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.repository.CaronaRepository;
import br.com.fatec.tcc.repository.ParticipacaoCaronaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CaronaService {

	private final CaronaRepository caronaRepository;
	private final ParticipacaoCaronaRepository participacaoRepository;
	private final UsuarioService usuarioService;

	@Transactional
	public CaronaResponseDTO oferecerCarona(CaronaRequestDTO request, String email) {
		Usuario motorista = usuarioService.findUserByUsername(email);

		Carona carona = new Carona();
		carona.setMotorista(motorista);
		carona.setOrigem(request.getOrigem());
		carona.setDestino(request.getDestino());
		carona.setHorarioSaida(request.getHorarioSaida());
		carona.setVagasDisponiveis(request.getVagasDisponiveis());
		carona.setVeiculoModelo(request.getVeiculoModelo());
		carona.setVeiculoPlaca(request.getVeiculoPlaca());
		carona.setObservacoes(request.getObservacoes());
		carona.setStatus(Carona.StatusCarona.ABERTA);

		Carona saved = caronaRepository.save(carona);
		return convertToResponseDTO(saved);
	}

	public List<CaronaResponseDTO> listarCaronasDisponiveis(String origem, String destino, LocalDateTime horarioInicio,
			LocalDateTime horarioFim) {
		return caronaRepository
				.buscarCaronasDisponiveis(LocalDateTime.now(), origem, destino, horarioInicio, horarioFim).stream()
				.map(this::convertToResponseDTO).collect(Collectors.toList());
	}

	@Transactional
	public void solicitarVaga(Long caronaId, String email) {
		Usuario passageiro = usuarioService.findUserByUsername(email);
		Carona carona = caronaRepository.findById(caronaId)
				.orElseThrow(() -> new RuntimeException("Carona não encontrada"));

		// Verificar se já existe solicitação
		if (participacaoRepository.findByCaronaAndPassageiro(carona, passageiro).isPresent()) {
			throw new RuntimeException("Você já solicitou esta carona");
		}

		// Verificar vagas disponíveis
		long vagasOcupadas = participacaoRepository.countByCaronaAndStatus(carona,
				ParticipacaoCarona.StatusParticipacao.CONFIRMADA);

		if (vagasOcupadas >= carona.getVagasDisponiveis()) {
			throw new RuntimeException("Não há vagas disponíveis");
		}

		ParticipacaoCarona participacao = new ParticipacaoCarona();
		participacao.setCarona(carona);
		participacao.setPassageiro(passageiro);
		participacao.setStatus(ParticipacaoCarona.StatusParticipacao.SOLICITADA);

		participacaoRepository.save(participacao);
	}

	@Transactional
	public void confirmarPassageiro(Long caronaId, Long participacaoId, String email) {
		Carona carona = caronaRepository.findById(caronaId)
				.orElseThrow(() -> new RuntimeException("Carona não encontrada"));
		Usuario motorista = usuarioService.findUserByUsername(email);

		if (!carona.getMotorista().getId().equals(motorista.getId())) {
			throw new RuntimeException("Apenas o motorista pode confirmar passageiros");
		}

		ParticipacaoCarona participacao = participacaoRepository.findById(participacaoId)
				.orElseThrow(() -> new RuntimeException("Participação não encontrada"));

		// Verificar vagas disponíveis
		long vagasOcupadas = participacaoRepository.countByCaronaAndStatus(carona,
				ParticipacaoCarona.StatusParticipacao.CONFIRMADA);

		if (vagasOcupadas >= carona.getVagasDisponiveis()) {
			throw new RuntimeException("Não há vagas disponíveis");
		}

		participacao.setStatus(ParticipacaoCarona.StatusParticipacao.CONFIRMADA);
		participacao.setDataConfirmacao(LocalDateTime.now());
		participacaoRepository.save(participacao);
	}

	private CaronaResponseDTO convertToResponseDTO(Carona carona) {
	    long vagasOcupadas = participacaoRepository.countByCaronaAndStatus(carona, ParticipacaoCarona.StatusParticipacao.CONFIRMADA);
	    return new CaronaResponseDTO(
	        carona.getId(),
	        carona.getMotorista().getNomeCompleto(),
	        carona.getOrigem(),
	        carona.getDestino(),
	        carona.getHorarioSaida(),
	        carona.getVagasDisponiveis(),
	        (int) vagasOcupadas,
	        carona.getVeiculoModelo(),
	        carona.getVeiculoPlaca(),
	        carona.getObservacoes(),
	        carona.getStatus()
	    );
	}

	public CaronaResponseDTO buscarPorId(Long id) {
		Carona carona = caronaRepository.findById(id).orElseThrow(() -> new RuntimeException("Carona não encontrada"));
		return convertToResponseDTO(carona);
	}
	
	public List<CaronaResponseDTO> listarCaronasPorUsuario(Usuario usuario) {
	    // Caronas oferecidas
	    List<CaronaResponseDTO> oferecidas = caronaRepository.findByMotoristaOrderByDataCriacaoDesc(usuario)
	            .stream().map(this::convertToResponseDTO).collect(Collectors.toList());
	    // Caronas solicitadas (participações)
	    List<CaronaResponseDTO> solicitadas = participacaoRepository.findByPassageiroOrderByDataSolicitacaoDesc(usuario)
	            .stream().map(p -> convertToResponseDTO(p.getCarona())).collect(Collectors.toList());
	    // Unir as duas listas (evitar duplicatas se necessário)
	    oferecidas.addAll(solicitadas);
	    return oferecidas;
	}
}
