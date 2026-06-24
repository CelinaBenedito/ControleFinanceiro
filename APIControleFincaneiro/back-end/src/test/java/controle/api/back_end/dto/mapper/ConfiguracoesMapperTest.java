package controle.api.back_end.dto.mapper;

import controle.api.back_end.dto.configuracoes.in.ConfiguracaoEditDTO;
import controle.api.back_end.dto.configuracoes.in.ConfiguracoesCreateDTO;
import controle.api.back_end.dto.configuracoes.mapper.ConfiguracoesMapper;
import controle.api.back_end.dto.configuracoes.out.ConfiguracaoUsuarioResponseDTO;
import controle.api.back_end.dto.configuracoes.out.ConfiguracoesResponsesDTO;
import controle.api.back_end.model.categoria.Categoria;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.configuracoes.LimitePorCategoria;
import controle.api.back_end.model.configuracoes.LimitePorInstituicao;
import controle.api.back_end.model.instituicao.Instituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.model.usuario.UsuarioSexo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfiguracoesMapper - testes unitários")
class ConfiguracoesMapperTest {

    private Configuracoes configuracoes;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNome("Joana");
        usuario.setSobrenome("Silva");
        usuario.setEmail("joana@email.com");
        usuario.setSexo(UsuarioSexo.Feminino);
        usuario.setDataNascimento(LocalDate.of(1998, 1, 15));

        configuracoes = new Configuracoes();
        configuracoes.setId(UUID.randomUUID());
        configuracoes.setUsuario(usuario);
        configuracoes.setInicioMesFiscal(1);
        configuracoes.setLimiteDesejadoMensal(2000.0);
        configuracoes.setUltimaAtualizacao(LocalDate.now());
        configuracoes.setLimitePorCategoria(List.of());
        configuracoes.setLimitePorInstituicao(List.of());
    }

    // ── toDto(model) ───────────────────────────────────────────────────────
    @Test
    @DisplayName("toDto: mapeia campos corretamente")
    void toDto_mapeiaCorretamente() {
        ConfiguracoesResponsesDTO dto = ConfiguracoesMapper.toDto(configuracoes);

        assertNotNull(dto);
        assertEquals(configuracoes.getId(), dto.getId());
        assertEquals(configuracoes.getInicioMesFiscal(), dto.getInicioMesFiscal());
        assertEquals(configuracoes.getLimiteDesejadoMensal(), dto.getLimiteDesejadoMensal());
        assertEquals(configuracoes.getUltimaAtualizacao(), dto.getUltimaAtualizacao());
    }

    @Test
    @DisplayName("toDto: retorna null para entrada null")
    void toDto_comNull_retornaNull() {
        assertNull(ConfiguracoesMapper.toDto((Configuracoes) null));
    }

    @Test
    @DisplayName("toDto(lista): mapeia todos os elementos")
    void toDtoLista_mapeiaLista() {
        List<ConfiguracoesResponsesDTO> resultado = ConfiguracoesMapper.toDto(List.of(configuracoes));
        assertEquals(1, resultado.size());
    }

    // ── toDtoUser ──────────────────────────────────────────────────────────
    @Test
    @DisplayName("toDtoUser: mapeia usuário e campos básicos")
    void toDtoUser_mapeiaUsuario() {
        ConfiguracaoUsuarioResponseDTO dto = ConfiguracoesMapper.toDtoUser(configuracoes);

        assertNotNull(dto);
        assertNotNull(dto.getUsuario());
        assertEquals(usuario.getId(), dto.getUsuario().getId());
        assertEquals(usuario.getNome(), dto.getUsuario().getNome());
        assertEquals(configuracoes.getInicioMesFiscal(), dto.getInicioMesFiscal());
        assertEquals(configuracoes.getLimiteDesejadoMensal(), dto.getLimiteDesejadoMensal());
    }

    @Test
    @DisplayName("toDtoUser: listas de limites são mapeadas quando preenchidas")
    void toDtoUser_mapeiaLimites() {
        // Monta LimitePorCategoria
        Categoria categoria = new Categoria(1, "Alimentação");
        CategoriaUsuario cu = new CategoriaUsuario();
        cu.setCategoria(categoria);

        LimitePorCategoria limiteCat = new LimitePorCategoria();
        limiteCat.setId(UUID.randomUUID());
        limiteCat.setCategoriaUsuario(cu);
        limiteCat.setLimiteDesejado(500.0);

        // Monta LimitePorInstituicao
        Instituicao inst = new Instituicao(1, "Nubank");
        InstituicaoUsuario iu = new InstituicaoUsuario();
        iu.setInstituicao(inst);

        LimitePorInstituicao limiteInst = new LimitePorInstituicao();
        limiteInst.setId(UUID.randomUUID());
        limiteInst.setInstituicaoUsuario(iu);
        limiteInst.setLimiteDesejado(800.0);

        configuracoes.setLimitePorCategoria(List.of(limiteCat));
        configuracoes.setLimitePorInstituicao(List.of(limiteInst));

        ConfiguracaoUsuarioResponseDTO dto = ConfiguracoesMapper.toDtoUser(configuracoes);

        assertEquals(1, dto.getLimitePorCategoria().size());
        assertEquals(500.0, dto.getLimitePorCategoria().get(0).getLimiteDesejado());
        assertEquals("Alimentação", dto.getLimitePorCategoria().get(0).getCategoria().getTitulo());

        assertEquals(1, dto.getLimiteInstituicao().size());
        assertEquals(800.0, dto.getLimiteInstituicao().get(0).getLimiteDesejado());
        assertEquals("Nubank", dto.getLimiteInstituicao().get(0).getInstituicao().getNome());
    }

    @Test
    @DisplayName("toDtoUser: retorna null para entrada null")
    void toDtoUser_comNull_retornaNull() {
        assertNull(ConfiguracoesMapper.toDtoUser((Configuracoes) null));
    }

    // ── toEntity(CreateDTO) ────────────────────────────────────────────────
    @Test
    @DisplayName("toEntity(CreateDTO): mapeia campos corretamente")
    void toEntity_createDto_mapeia() {
        UUID userId = UUID.randomUUID();
        ConfiguracoesCreateDTO dto = new ConfiguracoesCreateDTO();
        dto.setFkUsuario(userId);
        dto.setInicioMesFiscal(5);
        dto.setLimiteDesejadoMensal(1500.0);

        Configuracoes entity = ConfiguracoesMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals(userId, entity.getUsuario().getId());
        assertEquals(5, entity.getInicioMesFiscal());
        assertEquals(1500.0, entity.getLimiteDesejadoMensal());
    }

    @Test
    @DisplayName("toEntity(CreateDTO): retorna null para entrada null")
    void toEntity_createDtoNull_retornaNull() {
        assertNull(ConfiguracoesMapper.toEntity((ConfiguracoesCreateDTO) null));
    }

    // ── toEntity(EditDTO) ──────────────────────────────────────────────────
    @Test
    @DisplayName("toEntity(EditDTO): mapeia apenas campos não nulos")
    void toEntity_editDto_mapeia() {
        ConfiguracaoEditDTO dto = new ConfiguracaoEditDTO();
        dto.setInicioMesFiscal(10);
        dto.setLimiteDesejadoMensal(3000.0);

        Configuracoes entity = ConfiguracoesMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals(10, entity.getInicioMesFiscal());
        assertEquals(3000.0, entity.getLimiteDesejadoMensal());
    }

    @Test
    @DisplayName("toEntity(EditDTO): retorna null para entrada null")
    void toEntity_editDtoNull_retornaNull() {
        assertNull(ConfiguracoesMapper.toEntity((ConfiguracaoEditDTO) null));
    }
}

