package controle.api.back_end.service;

import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.exception.MenorDeIdadeException;
import controle.api.back_end.exception.SenhasNaoCoincidemException;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.model.usuario.UsuarioSexo;
import controle.api.back_end.repository.categoria.CategoriaUsuarioRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoFinanceiroRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - testes unitários")
class UsuarioServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock ConfiguracoesService configuracoesService;
    @Mock EventoFinanceiroRepository eventoFinanceiroRepository;
    @Mock InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    @Mock CategoriaUsuarioRepository categoriaUsuarioRepository;

    @InjectMocks UsuarioService usuarioService;

    private Usuario usuarioBase;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        usuarioBase = new Usuario();
        usuarioBase.setId(userId);
        usuarioBase.setNome("Ana");
        usuarioBase.setSobrenome("Costa");
        usuarioBase.setEmail("ana@email.com");
        usuarioBase.setSenha("Senha@123");
        usuarioBase.setSexo(UsuarioSexo.Feminino);
        usuarioBase.setDataNascimento(LocalDate.of(2000, 5, 10));
    }

    // ── getUsuarioById ─────────────────────────────────────────────────────
    @Test
    @DisplayName("getUsuarioById: retorna usuário quando existe")
    void getUsuarioById_retornaUsuario() {
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuarioBase));

        Usuario resultado = usuarioService.getUsuarioById(userId);

        assertNotNull(resultado);
        assertEquals(userId, resultado.getId());
    }

    @Test
    @DisplayName("getUsuarioById: lança exceção quando não encontrado")
    void getUsuarioById_lancaExcecao() {
        when(usuarioRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class,
                () -> usuarioService.getUsuarioById(userId));
    }

    // ── ageValidation ──────────────────────────────────────────────────────
    @Test
    @DisplayName("ageValidation: retorna true para usuário com 18+ anos")
    void ageValidation_maiorDeIdade_retornaTrue() {
        LocalDate nascimento = LocalDate.now().minusYears(20);
        assertTrue(usuarioService.ageValidation(nascimento));
    }

    @Test
    @DisplayName("ageValidation: retorna false para usuário com menos de 18 anos")
    void ageValidation_menorDeIdade_retornaFalse() {
        LocalDate nascimento = LocalDate.now().minusYears(16);
        assertFalse(usuarioService.ageValidation(nascimento));
    }

    @Test
    @DisplayName("ageValidation: retorna true para usuário com exatamente 18 anos")
    void ageValidation_exatamente18Anos_retornaTrue() {
        LocalDate nascimento = LocalDate.now().minusYears(18);
        assertTrue(usuarioService.ageValidation(nascimento));
    }

    // ── createUsuario ──────────────────────────────────────────────────────
    @Test
    @DisplayName("createUsuario: salva quando maior de idade")
    void createUsuario_maiorDeIdade_salva() {
        when(usuarioRepository.save(usuarioBase)).thenReturn(usuarioBase);

        Usuario resultado = usuarioService.createUsuario(usuarioBase);

        assertEquals(usuarioBase, resultado);
        verify(usuarioRepository).save(usuarioBase);
    }

    @Test
    @DisplayName("createUsuario: lança MenorDeIdadeException para menor de 18 anos")
    void createUsuario_menorDeIdade_lancaExcecao() {
        usuarioBase.setDataNascimento(LocalDate.now().minusYears(15));

        assertThrows(MenorDeIdadeException.class, () -> usuarioService.createUsuario(usuarioBase));
        verify(usuarioRepository, never()).save(any());
    }

    // ── editSenhaByUserId ──────────────────────────────────────────────────
    @Test
    @DisplayName("editSenhaByUserId: altera senha quando senha antiga está correta")
    void editSenhaByUserId_senhaCorreta_alteraSenha() {
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuarioBase));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = usuarioService.editSenhaByUserId(userId, "NovaSenha@1", "Senha@123");

        assertEquals("NovaSenha@1", resultado.getSenha());
    }

    @Test
    @DisplayName("editSenhaByUserId: lança SenhasNaoCoincidemException quando senha antiga é inválida")
    void editSenhaByUserId_senhaErrada_lancaExcecao() {
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuarioBase));

        assertThrows(SenhasNaoCoincidemException.class,
                () -> usuarioService.editSenhaByUserId(userId, "Nova@1", "SenhaErrada"));
    }

    // ── getXpByUserId ──────────────────────────────────────────────────────
    @Test
    @DisplayName("getXpByUserId: lança exceção quando usuário não existe")
    void getXpByUserId_usuarioNaoExiste_lancaExcecao() {
        when(usuarioRepository.existsById(userId)).thenReturn(false);

        assertThrows(EntidadeNaoEncontradaException.class,
                () -> usuarioService.getXpByUserId(userId));
    }

    @Test
    @DisplayName("getXpByUserId: calcula XP corretamente")
    void getXpByUserId_calculaXp() {
        when(usuarioRepository.existsById(userId)).thenReturn(true);

        // 10 registros, 2 instituições, 4 categorias
        List<EventoFinanceiro> eventos = List.of(
                criarEvento(), criarEvento(), criarEvento(), criarEvento(), criarEvento(),
                criarEvento(), criarEvento(), criarEvento(), criarEvento(), criarEvento()
        );
        when(eventoFinanceiroRepository.findEventoFinanceiroByUsuario_Id(userId)).thenReturn(eventos);
        when(instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(userId))
                .thenReturn(List.of(mock(controle.api.back_end.model.instituicao.InstituicaoUsuario.class),
                                    mock(controle.api.back_end.model.instituicao.InstituicaoUsuario.class)));
        when(categoriaUsuarioRepository.findAllByUsuario_IdAndIsAtivoIsTrue(userId))
                .thenReturn(List.of(
                        mock(controle.api.back_end.model.categoria.CategoriaUsuario.class),
                        mock(controle.api.back_end.model.categoria.CategoriaUsuario.class),
                        mock(controle.api.back_end.model.categoria.CategoriaUsuario.class),
                        mock(controle.api.back_end.model.categoria.CategoriaUsuario.class)));

        Double xp = usuarioService.getXpByUserId(userId);

        // 10 registros → (10/10)*100 = 100
        // 2 instituições → 2*100 = 200
        // 4 categorias   → 4*50  = 200
        // total = 500
        assertEquals(500.0, xp);
    }

    // ── deleteUserbyId / activateUsuario ───────────────────────────────────
    @Test
    @DisplayName("deleteUserbyId: marca usuário como inativo")
    void deleteUserbyId_marcaInativo() {
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuarioBase));

        usuarioService.deleteUserbyId(userId);

        assertFalse(usuarioBase.getIsAtivo());
    }

    @Test
    @DisplayName("activateUsuario: marca usuário como ativo")
    void activateUsuario_marcaAtivo() {
        usuarioBase.setIsAtivo(false);
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuarioBase));

        Usuario resultado = usuarioService.activateUsuario(userId);

        assertTrue(resultado.getIsAtivo());
    }

    // ── helper ─────────────────────────────────────────────────────────────
    private EventoFinanceiro criarEvento() {
        EventoFinanceiro e = new EventoFinanceiro();
        e.setId(UUID.randomUUID());
        return e;
    }
}

