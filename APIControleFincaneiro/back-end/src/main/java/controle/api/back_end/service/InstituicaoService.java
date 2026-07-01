package controle.api.back_end.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import controle.api.back_end.dto.instituicao.in.InstituicaoOFXConfigUpdateDTO;
import controle.api.back_end.dto.instituicao.out.InstituicaoOFXConfigDTO;
import controle.api.back_end.dto.ofx.in.NavigationStepDTO;
import controle.api.back_end.exception.EntidadeJaExisteException;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.instituicao.Instituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.eventoFinanceiro.EventoFinanceiroRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoInstituicaoRepository;
import controle.api.back_end.repository.instituicao.InstituicaoRepository;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InstituicaoService {
    private final InstituicaoRepository instituicaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final EventoInstituicaoRepository eventoInstituicaoRepository;
    private final EventoFinanceiroRepository eventoFinanceiroRepository;
    private final UsuarioService usuarioService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InstituicaoService(InstituicaoRepository instituicaoRepository,
                              UsuarioRepository usuarioRepository,
                              InstituicaoUsuarioRepository instituicaoUsuarioRepository,
                              EventoInstituicaoRepository eventoInstituicaoRepository,
                              EventoFinanceiroRepository eventoFinanceiroRepository,
                              UsuarioService usuarioService) {
        this.instituicaoRepository = instituicaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
        this.usuarioService = usuarioService;
    }

    public Page<Instituicao> getInstituicoes(Pageable pageable) {
        return instituicaoRepository.findAll(pageable);
    }

    public Instituicao getInstituicaoById(Integer id) {
        return instituicaoRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Instituicao de id: %d nao encontrada".formatted(id)
                ));
    }

    public Instituicao createInstituicao(Instituicao entity) {
        Instituicao instituicaoByNomeContainingIgnoreCase = instituicaoRepository.findInstituicaoByNomeContainingIgnoreCase(entity.getNome());
        if (instituicaoByNomeContainingIgnoreCase != null) {
            throw new EntidadeJaExisteException("Ja existe uma instituicao com o nome %s no banco de dados".formatted(entity.getNome()));
        }
        return instituicaoRepository.save(entity);
    }

    public void deleteInstituicao(Integer id) {
        if (instituicaoRepository.existsById(id)) {
            instituicaoRepository.deleteInstituicaoById(id);
        } else {
            throw new EntidadeNaoEncontradaException("Instituicao de id: %d nao encontrada.".formatted(id));
        }
    }

    public InstituicaoUsuario createInstituicaoForUsuario(Integer instituicao_id, UUID user_id) {
        if (!usuarioRepository.existsById(user_id)) {
            throw new EntidadeNaoEncontradaException("Usuario de id: %s nao encontrado".formatted(user_id));
        }
        if (!instituicaoRepository.existsById(instituicao_id)) {
            throw new EntidadeNaoEncontradaException("Instituicao de id: %s nao encontrado".formatted(instituicao_id));
        }
        Optional<Usuario> user = usuarioRepository.findById(user_id);
        Optional<Instituicao> instituicao = instituicaoRepository.findById(instituicao_id);
        if (instituicaoUsuarioRepository.existsByUsuarioAndInstituicao(user.get(), instituicao.get())) {
            throw new IllegalArgumentException("Usuario ja vinculado a esta instituicao.");
        }
        InstituicaoUsuario instituicaoUsuario = new InstituicaoUsuario();
        instituicaoUsuario.setInstituicao(instituicao.get());
        instituicaoUsuario.setUsuario(user.get());
        instituicaoUsuario.setUltimaModificacao(LocalDateTime.now());
        instituicaoUsuario.setIsAtivo(true);
        return instituicaoUsuarioRepository.save(instituicaoUsuario);
    }

    public Page<InstituicaoUsuario> getInstituicoesByUserId(UUID idUser, Pageable pageable) {
        if (!usuarioRepository.existsById(idUser)) {
            throw new EntidadeNaoEncontradaException("Usuario de id: %s nao encontrado".formatted(idUser));
        }
        return instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(idUser, pageable);
    }

    public InstituicaoUsuario detachUserFromInstituicao(Integer instituicaoId, UUID userId) {
        if (!usuarioRepository.existsById(userId)) {
            throw new EntidadeNaoEncontradaException("Usuario de id: %s nao encontrado".formatted(userId));
        }
        if (!instituicaoRepository.existsById(instituicaoId)) {
            throw new EntidadeNaoEncontradaException("Instituicao de id: %d nao encontrado".formatted(instituicaoId));
        }
        InstituicaoUsuario instituicaoUsuario = instituicaoUsuarioRepository.findByUsuario_IdAndInstituicao_Id(userId, instituicaoId);
        instituicaoUsuario.setIsAtivo(false);
        instituicaoUsuario.setUltimaModificacao(LocalDateTime.now());
        return instituicaoUsuarioRepository.save(instituicaoUsuario);
    }

    public BigDecimal getSaldoByInstituicao(Integer instituicaoUsuarioId) {
        instituicaoUsuarioRepository.findById(instituicaoUsuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Associacao de instituicao e usuario de id: %d nao encontrada.".formatted(instituicaoUsuarioId)
                ));
        List<EventoInstituicao> eventosInstituicao =
                eventoInstituicaoRepository.findByInstituicaoUsuario_Id(instituicaoUsuarioId);
        BigDecimal saldo = BigDecimal.ZERO;
        for (EventoInstituicao eventoInstituicao : eventosInstituicao) {
            EventoFinanceiro eventoFinanceiro = eventoInstituicao.getEventoFinanceiro();
            if (eventoFinanceiro == null) continue;
            saldo = getSaldo(saldo, eventoFinanceiro);
        }
        return saldo;
    }

    static BigDecimal getSaldo(BigDecimal saldo, EventoFinanceiro eventoFinanceiro) {
        BigDecimal valor = BigDecimal.valueOf(eventoFinanceiro.getValor());
        if (eventoFinanceiro.getTipo() == Tipo.Gasto || eventoFinanceiro.getTipo() == Tipo.Transferencia) {
            saldo = saldo.subtract(valor);
        } else if (eventoFinanceiro.getTipo() == Tipo.Recebimento) {
            saldo = saldo.add(valor);
        }
        return saldo;
    }

    public void detachAllIntituicoes(UUID userId) {
        usuarioService.getUsuario(userId);
        List<InstituicaoUsuario> instituicoesUsuarios =
                instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(userId);
        for (InstituicaoUsuario instituicao : instituicoesUsuarios) {
            instituicao.setIsAtivo(false);
            instituicaoUsuarioRepository.save(instituicao);
        }
    }

    // =========================================================================
    // OFX Config
    // =========================================================================

    public InstituicaoOFXConfigDTO getOFXConfig(Integer id) {
        Instituicao inst = getInstituicaoById(id);
        List<NavigationStepDTO> steps = parseSteps(inst.getNavigationStepsJson());
        List<String> placeholders = extrairPlaceholders(inst.getNavigationStepsJson());
        return new InstituicaoOFXConfigDTO(
                inst.getId(), inst.getNome(),
                Boolean.TRUE.equals(inst.getOfxSupported()),
                inst.getLoginMode() != null ? inst.getLoginMode() : Instituicao.LoginMode.MANUAL,
                inst.getBankUrl(),
                inst.getPythonEndpoint() != null ? inst.getPythonEndpoint() : "/capture",
                steps, placeholders
        );
    }

    public InstituicaoOFXConfigDTO updateOFXConfig(Integer id, InstituicaoOFXConfigUpdateDTO dto) {
        Instituicao inst = getInstituicaoById(id);
        if (dto.ofxSupported() != null) inst.setOfxSupported(dto.ofxSupported());
        if (dto.bankUrl() != null) inst.setBankUrl(dto.bankUrl());
        if (dto.loginMode() != null) inst.setLoginMode(dto.loginMode());
        if (dto.pythonEndpoint() != null) inst.setPythonEndpoint(dto.pythonEndpoint());
        if (dto.navigationSteps() != null) {
            try {
                inst.setNavigationStepsJson(objectMapper.writeValueAsString(dto.navigationSteps()));
            } catch (Exception e) {
                throw new RuntimeException("Erro ao serializar navigationSteps: " + e.getMessage());
            }
        } else {
            inst.setNavigationStepsJson(null);
        }
        instituicaoRepository.save(inst);
        return getOFXConfig(id);
    }

    public List<InstituicaoOFXConfigDTO> getTodasOFXConfigs() {
        return instituicaoRepository.findAll().stream()
                .map(inst -> {
                    List<NavigationStepDTO> steps = parseSteps(inst.getNavigationStepsJson());
                    List<String> placeholders = extrairPlaceholders(inst.getNavigationStepsJson());
                    return new InstituicaoOFXConfigDTO(
                            inst.getId(), inst.getNome(),
                            Boolean.TRUE.equals(inst.getOfxSupported()),
                            inst.getLoginMode() != null ? inst.getLoginMode() : Instituicao.LoginMode.MANUAL,
                            inst.getBankUrl(),
                            inst.getPythonEndpoint() != null ? inst.getPythonEndpoint() : "/capture",
                            steps, placeholders
                    );
                })
                .toList();
    }

    private List<NavigationStepDTO> parseSteps(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> extrairPlaceholders(String json) {
        if (json == null || json.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        Matcher m = Pattern.compile("\\{\\{([A-Z_]+)}}").matcher(json);
        while (m.find()) {
            String p = m.group(1);
            if (!result.contains(p)) result.add(p);
        }
        return result;
    }
}
