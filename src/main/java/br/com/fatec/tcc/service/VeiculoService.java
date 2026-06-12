package br.com.fatec.tcc.service;

import br.com.fatec.tcc.config.FrotaBrasileira;
import br.com.fatec.tcc.dto.ModeloCustomDTO;
import br.com.fatec.tcc.dto.VeiculoDTO;
import br.com.fatec.tcc.dto.VeiculoRequestDTO;
import br.com.fatec.tcc.model.ModeloCustom;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.model.Veiculo;
import br.com.fatec.tcc.repository.ModeloCustomRepository;
import br.com.fatec.tcc.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final ModeloCustomRepository modeloCustomRepository;
    private final ModeloCustomPersister modeloCustomPersister;
    private final UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public List<VeiculoDTO> listarVeiculosDoUsuario(String email) {
        Usuario usuario = usuarioService.findUserByUsername(email);
        return veiculoRepository.findByUsuarioIdAndAtivoTrueOrderByDataCadastroDesc(usuario.getId())
                .stream().map(this::toDTO).toList();
    }

    @Transactional
    public VeiculoDTO cadastrarVeiculo(VeiculoRequestDTO req, String email) {
        Usuario usuario = usuarioService.findUserByUsername(email);

        String placa = req.placa().toUpperCase().replace("-", "");
        if (veiculoRepository.existsByPlaca(placa)) {
            throw new RuntimeException("Essa placa já está cadastrada no sistema.");
        }

        boolean marcaConhecida = FrotaBrasileira.marcaExiste(req.marca());
        boolean modeloConhecido = marcaConhecida && FrotaBrasileira.modeloExiste(req.marca(), req.modelo());

        if (!marcaConhecida || !modeloConhecido) {
            // Verifica se já existe solicitação pendente igual
            if (modeloCustomRepository.existsByMarcaIgnoreCaseAndModeloIgnoreCaseAndStatus(
                    req.marca(), req.modelo(), ModeloCustom.Status.PENDENTE)) {
                throw new RuntimeException(
                    "Já existe uma solicitação pendente para esse modelo. Aguarde a aprovação do administrador.");
            }
            // Salva em transação independente para não ser revertido junto com a exceção
            modeloCustomPersister.salvar(usuario, req.marca(), req.modelo());
            log.info("Modelo custom solicitado por {}: {} {}", email, req.marca(), req.modelo());
            throw new RuntimeException(
                "CUSTOM_PENDENTE:" + req.marca() + " / " + req.modelo()
                    + " não consta no catálogo. Sua solicitação foi enviada para aprovação do administrador.");
        }

        Veiculo veiculo = new Veiculo();
        veiculo.setUsuario(usuario);
        veiculo.setMarca(req.marca());
        veiculo.setModelo(req.modelo());
        veiculo.setPlaca(placa);
        veiculo.setAno(req.ano());
        veiculo.setCor(req.cor());
        Veiculo saved = veiculoRepository.save(veiculo);
        log.info("Veiculo cadastrado por {}: {} {} {}", email, req.marca(), req.modelo(), placa);
        return toDTO(saved);
    }

    @Transactional
    public void removerVeiculo(Long id, String email) {
        Usuario usuario = usuarioService.findUserByUsername(email);
        Veiculo veiculo = veiculoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado."));
        if (!veiculo.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("Sem permissão para remover este veículo.");
        }
        veiculo.setAtivo(false);
        veiculoRepository.save(veiculo);
    }

    // ── Admin: modelos custom ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ModeloCustomDTO> listarModelosCustom(String status) {
        List<ModeloCustom> lista = (status == null || status.isBlank())
                ? modeloCustomRepository.findAllByOrderByDataSolicitacaoDesc()
                : modeloCustomRepository.findByStatusOrderByDataSolicitacaoDesc(
                    ModeloCustom.Status.valueOf(status.toUpperCase()));
        return lista.stream().map(this::toCustomDTO).toList();
    }

    @Transactional
    public ModeloCustomDTO resolverModeloCustom(Long id, String decisao, String observacao) {
        ModeloCustom custom = modeloCustomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada."));
        ModeloCustom.Status novoStatus = ModeloCustom.Status.valueOf(decisao.toUpperCase());
        custom.setStatus(novoStatus);
        custom.setObservacaoAdmin(observacao);
        custom.setDataResolucao(LocalDateTime.now());
        modeloCustomRepository.save(custom);
        log.info("Modelo custom {} {} -> {} por admin", custom.getMarca(), custom.getModelo(), novoStatus);
        return toCustomDTO(custom);
    }

    public Map<String, List<String>> getCatalogo() {
        return FrotaBrasileira.CATALOGO;
    }

    private VeiculoDTO toDTO(Veiculo v) {
        return new VeiculoDTO(v.getId(), v.getMarca(), v.getModelo(), v.getPlaca(),
                v.getAno(), v.getCor(), v.isAtivo(), v.getDataCadastro());
    }

    private ModeloCustomDTO toCustomDTO(ModeloCustom c) {
        return new ModeloCustomDTO(
                c.getId(),
                c.getSolicitante().getNomeCompleto(),
                c.getSolicitante().getEmail(),
                c.getMarca(), c.getModelo(),
                c.getStatus().name(),
                c.getObservacaoAdmin(),
                c.getDataSolicitacao(),
                c.getDataResolucao());
    }
}
