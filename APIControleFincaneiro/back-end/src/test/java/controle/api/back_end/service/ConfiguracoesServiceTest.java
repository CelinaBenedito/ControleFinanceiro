package controle.api.back_end.service;

import controle.api.back_end.dto.configuracoes.in.PeriodoTempoRequestDto;
import controle.api.back_end.exception.EntidadeJaExisteException;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.configuracoes.LimitePorCategoria;
import controle.api.back_end.model.configuracoes.LimitePorInstituicao;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.categoria.CategoriaUsuarioRepository;
import controle.api.back_end.repository.configuracoes.ConfiguracoesRepository;
import controle.api.back_end.repository.configuracoes.LimitePorCategoriaRepository;
import controle.api.back_end.repository.configuracoes.LimitePorInstituicaoRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoDetalheRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoFinanceiroRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoInstituicaoRepository;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfiguracoesService - testes unitários")
class ConfiguracoesServiceTest {

    @Mock ConfiguracoesRepository configuracoesRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock CategoriaUsuarioRepository categoriaUsuarioRepository;
    @Mock LimitePorCategoriaRepository limitePorCategoriaRepository;
    @Mock InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    @Mock LimitePorInstituicaoRepository limitePorInstiuicaoRepository;
    @Mock EventoFinanceiroRepository eventoFinanceiroRepository;
    @Mock EventoInstituicaoRepository eventoInstituicaoRepository;
    @Mock EventoDetalheRepository eventoDetalheRepository;

    @InjectMocks ConfiguracoesService configuracoesService;

    private UUID userId;
    private UUID configId;
    private Usuario usuario;
    private Configuracoes configuracoes;

    @BeforeEach
    void setUp() {
        userId    = UUID.randomUUID();
        configId  = UUID.randomUUID();

        usuario = new Usuario();
        usuario.setId(userId);
        usuario.setNome("Lucas");
        usuario.setSobrenome("Melo");
        usuario.setEmail("lucas@email.com");

        configuracoes = new Configuracoes();
        configuracoes.setId(configId);
        configuracoes.setUsuario(usuario);
        configuracoes.setInicioMesFiscal(1);
        configuracoes.setLimiteDesejadoMensal(2500.0);
        configuracoes.setUltimaAtualizacao(LocalDate.now());
    }

    // ── getConfiguracoes ───────────────────────────────────────────────────
    @Test
    @DisplayName("getConfiguracoes: retorna todas as configurações")
    void getConfiguracoes_retornaLista() {
        when(configuracoesRepository.findAll()).thenReturn(List.of(configuracoes));

        List<Configuracoes> resultado = configuracoesService.getConfiguracoes();

        assertEquals(1, resultado.size());
    }

    // ── createConfiguracao ─────────────────────────────────────────────────
    @Test
    @DisplayName("createConfiguracao: cria quando usuário existe e não tem config")
    void createConfiguracao_cria() {
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(configuracoesRepository.existsConfiguracoesByUsuario_Id(userId)).thenReturn(false);
        when(configuracoesRepository.save(any())).thenReturn(configuracoes);

        Configuracoes resultado = configuracoesService.createConfiguracao(configuracoes, userId);

        assertNotNull(resultado);
        verify(configuracoesRepository).save(any());
    }

    @Test
    @DisplayName("createConfiguracao: lança EntidadeNaoEncontradaException quando usuário não existe")
    void createConfiguracao_usuarioNaoExiste_lancaExcecao() {
        when(usuarioRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class,
                () -> configuracoesService.createConfiguracao(configuracoes, userId));
    }

    @Test
    @DisplayName("createConfiguracao: lança EntidadeJaExisteException quando já existe config")
    void createConfiguracao_jaExiste_lancaExcecao() {
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(configuracoesRepository.existsConfiguracoesByUsuario_Id(userId)).thenReturn(true);

        assertThrows(EntidadeJaExisteException.class,
                () -> configuracoesService.createConfiguracao(configuracoes, userId));
    }

    // ── getConfiguracoesById ───────────────────────────────────────────────
    @Test
    @DisplayName("getConfiguracoesById: retorna configuração encontrada")
    void getConfiguracoesById_retornaConfiguracao() {
        when(configuracoesRepository.findById(configId)).thenReturn(Optional.of(configuracoes));
        when(limitePorCategoriaRepository.findLimitePorCategoriaByConfiguracoes_Id(configId))
                .thenReturn(Collections.emptyList());
        when(limitePorInstiuicaoRepository.findLimitePorInstituicaoByConfiguracoes_Id(configId))
                .thenReturn(Collections.emptyList());

        Configuracoes resultado = configuracoesService.getConfiguracoesById(configId);

        assertNotNull(resultado);
        assertEquals(configId, resultado.getId());
    }

    @Test
    @DisplayName("getConfiguracoesById: lança exceção quando não encontrada")
    void getConfiguracoesById_naoEncontrada_lancaExcecao() {
        when(configuracoesRepository.findById(configId)).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class,
                () -> configuracoesService.getConfiguracoesById(configId));
    }

    // ── getConfiguracaoByUserId ────────────────────────────────────────────
    @Test
    @DisplayName("getConfiguracaoByUserId: retorna configuração do usuário")
    void getConfiguracaoByUserId_retornaConfiguracao() {
        when(configuracoesRepository.findConfiguracoesByUsuario_Id(userId))
                .thenReturn(Optional.of(configuracoes));
        when(limitePorCategoriaRepository.findLimitePorCategoriaByConfiguracoes_Id(configId))
                .thenReturn(Collections.emptyList());
        when(limitePorInstiuicaoRepository.findLimitePorInstituicaoByConfiguracoes_Id(configId))
                .thenReturn(Collections.emptyList());

        Configuracoes resultado = configuracoesService.getConfiguracaoByUserId(userId);

        assertNotNull(resultado);
        assertEquals(userId, resultado.getUsuario().getId());
    }

    @Test
    @DisplayName("getConfiguracaoByUserId: lança exceção quando não encontrada")
    void getConfiguracaoByUserId_naoEncontrada_lancaExcecao() {
        when(configuracoesRepository.findConfiguracoesByUsuario_Id(userId))
                .thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class,
                () -> configuracoesService.getConfiguracaoByUserId(userId));
    }

    // ── deleteByPeriodoDeTempo ─────────────────────────────────────────────
    @Test
    @DisplayName("deleteByPeriodoDeTempo: lança exceção quando nenhum evento no período")
    void deleteByPeriodoDeTempo_semEventos_lancaExcecao() {
        when(configuracoesRepository.findById(configId)).thenReturn(Optional.of(configuracoes));
        when(limitePorCategoriaRepository.findLimitePorCategoriaByConfiguracoes_Id(any()))
                .thenReturn(Collections.emptyList());
        when(limitePorInstiuicaoRepository.findLimitePorInstituicaoByConfiguracoes_Id(any()))
                .thenReturn(Collections.emptyList());
        when(eventoFinanceiroRepository.findEventoFinanceiroByUsuario_Id(userId))
                .thenReturn(Collections.emptyList());

        PeriodoTempoRequestDto periodoDto = new PeriodoTempoRequestDto();
        periodoDto.setDataInical(LocalDate.of(2025, 1, 1));
        periodoDto.setDataFinal(LocalDate.of(2025, 12, 31));

        assertThrows(EntidadeNaoEncontradaException.class,
                () -> configuracoesService.deleteByPeriodoDeTempo(configId, periodoDto));
    }

    @Test
    @DisplayName("deleteByPeriodoDeTempo: deleta eventos dentro do período")
    void deleteByPeriodoDeTempo_deletaEventos() {
        EventoFinanceiro evento = new EventoFinanceiro();
        evento.setId(UUID.randomUUID());
        evento.setUsuario(usuario);
        evento.setDataEvento(LocalDate.of(2025, 6, 15));

        when(configuracoesRepository.findById(configId)).thenReturn(Optional.of(configuracoes));
        when(limitePorCategoriaRepository.findLimitePorCategoriaByConfiguracoes_Id(any()))
                .thenReturn(Collections.emptyList());
        when(limitePorInstiuicaoRepository.findLimitePorInstituicaoByConfiguracoes_Id(any()))
                .thenReturn(Collections.emptyList());
        when(eventoFinanceiroRepository.findEventoFinanceiroByUsuario_Id(userId))
                .thenReturn(List.of(evento));

        PeriodoTempoRequestDto periodoDto = new PeriodoTempoRequestDto();
        periodoDto.setDataInical(LocalDate.of(2025, 1, 1));
        periodoDto.setDataFinal(LocalDate.of(2025, 12, 31));

        assertDoesNotThrow(() -> configuracoesService.deleteByPeriodoDeTempo(configId, periodoDto));
        verify(eventoFinanceiroRepository).deleteAll(anyList());
    }

    // ── createLimitePorCategoria / Instituicao ─────────────────────────────
    @Test
    @DisplayName("createLimitePorCategoria: salva e retorna o limite")
    void createLimitePorCategoria_salva() {
        LimitePorCategoria limite = new LimitePorCategoria();
        controle.api.back_end.model.categoria.CategoriaUsuario cu =
                new controle.api.back_end.model.categoria.CategoriaUsuario();
        limite.setCategoriaUsuario(cu);
        limite.setLimiteDesejado(300.0);

        when(limitePorCategoriaRepository.save(any())).thenReturn(limite);

        LimitePorCategoria resultado =
                configuracoesService.createLimitePorCategoria(cu, 300.0);

        assertNotNull(resultado);
        assertEquals(300.0, resultado.getLimiteDesejado());
    }

    @Test
    @DisplayName("createLimitePorInstituicao: salva e retorna o limite")
    void createLimitePorInstituicao_salva() {
        controle.api.back_end.model.instituicao.InstituicaoUsuario iu =
                new controle.api.back_end.model.instituicao.InstituicaoUsuario();

        LimitePorInstituicao limite = new LimitePorInstituicao();
        limite.setInstituicaoUsuario(iu);
        limite.setLimiteDesejado(700.0);

        when(limitePorInstiuicaoRepository.findLimitePorInstituicaoByInstituicaoUsuario_Id(any()))
                .thenReturn(Collections.emptyList());
        when(limitePorInstiuicaoRepository.save(any())).thenReturn(limite);

        LimitePorInstituicao resultado =
                configuracoesService.createLimitePorInstituicao(iu, 700.0);

        assertNotNull(resultado);
        assertEquals(700.0, resultado.getLimiteDesejado());
    }
}

