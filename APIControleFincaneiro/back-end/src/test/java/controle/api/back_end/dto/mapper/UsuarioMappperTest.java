package controle.api.back_end.dto.mapper;

import controle.api.back_end.dto.usuario.in.UsuarioCreateDTO;
import controle.api.back_end.dto.usuario.in.UsuarioEditDTO;
import controle.api.back_end.dto.usuario.mapper.UsuarioMappper;
import controle.api.back_end.dto.usuario.out.UsuarioResponseDTO;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.model.usuario.UsuarioSexo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UsuarioMappper - testes unitários")
class UsuarioMappperTest {

    // ── helpers ────────────────────────────────────────────────────────────
    private Usuario criarUsuario() {
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setNome("Ana");
        u.setSobrenome("Costa");
        u.setEmail("ana@email.com");
        u.setSexo(UsuarioSexo.Feminino);
        u.setDataNascimento(LocalDate.of(2000, 5, 10));
        u.setSenha("Senha@123");
        u.setImagem("/img/foto.png");
        return u;
    }

    // ── toDto(Usuario) ─────────────────────────────────────────────────────
    @Test
    @DisplayName("toDto: mapeia todos os campos corretamente")
    void toDto_mapeiaCorretamente() {
        Usuario u = criarUsuario();
        UsuarioResponseDTO dto = UsuarioMappper.toDto(u);

        assertNotNull(dto);
        assertEquals(u.getId(), dto.getId());
        assertEquals(u.getNome(), dto.getNome());
        assertEquals(u.getSobrenome(), dto.getSobrenome());
        assertEquals(u.getEmail(), dto.getEmail());
        assertEquals(u.getSexo(), dto.getSexo());
        assertEquals(u.getDataNascimento(), dto.getDataNascimento());
        assertEquals(u.getImagem(), dto.getImagem());
    }

    @Test
    @DisplayName("toDto: retorna null para entrada null")
    void toDto_comNull_retornaNull() {
        assertNull(UsuarioMappper.toDto((Usuario) null));
    }

    @Test
    @DisplayName("toDto(lista): mapeia todos os elementos")
    void toDtoLista_mapeiaLista() {
        List<Usuario> lista = List.of(criarUsuario(), criarUsuario());
        List<UsuarioResponseDTO> resultado = UsuarioMappper.toDto(lista);
        assertEquals(2, resultado.size());
    }

    // ── toEntity(UsuarioCreateDTO) ─────────────────────────────────────────
    @Test
    @DisplayName("toEntity(CreateDTO): mapeia campos obrigatórios")
    void toEntity_createDto_mapeia() {
        UsuarioCreateDTO dto = new UsuarioCreateDTO();
        dto.setNome("Carlos");
        dto.setSobrenome("Lima");
        dto.setEmail("carlos@email.com");
        dto.setSexo(UsuarioSexo.Masculino);
        dto.setDataNascimento(LocalDate.of(1995, 3, 20));
        dto.setSenha("Pass@1234");

        Usuario entity = UsuarioMappper.toEntity(dto);

        assertNotNull(entity);
        assertEquals("Carlos", entity.getNome());
        assertEquals("Lima", entity.getSobrenome());
        assertEquals("carlos@email.com", entity.getEmail());
        assertEquals(UsuarioSexo.Masculino, entity.getSexo());
        assertEquals(LocalDate.of(1995, 3, 20), entity.getDataNascimento());
        assertEquals("Pass@1234", entity.getSenha());
    }

    @Test
    @DisplayName("toEntity(CreateDTO): retorna null para entrada null")
    void toEntity_createDtoNull_retornaNull() {
        assertNull(UsuarioMappper.toEntity((UsuarioCreateDTO) null));
    }

    // ── toEdit ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("toEdit: mantém campos do usuário atual quando editUser traz null")
    void toEdit_mantémCamposAtuaisQuandoNull() {
        Usuario atual = criarUsuario();
        Usuario editar = new Usuario(); // tudo null

        Usuario resultado = UsuarioMappper.toEdit(editar, atual);

        assertEquals(atual.getId(), resultado.getId());
        assertEquals(atual.getNome(), resultado.getNome());
        assertEquals(atual.getSobrenome(), resultado.getSobrenome());
        assertEquals(atual.getEmail(), resultado.getEmail());
        assertEquals(atual.getSexo(), resultado.getSexo());
        assertEquals(atual.getDataNascimento(), resultado.getDataNascimento());
        assertEquals(atual.getSenha(), resultado.getSenha());
    }

    @Test
    @DisplayName("toEdit: sobrescreve campos quando editUser traz valores novos")
    void toEdit_sobrescreveCampos() {
        Usuario atual = criarUsuario();
        Usuario editar = new Usuario();
        editar.setNome("NovoNome");
        editar.setEmail("novo@email.com");

        Usuario resultado = UsuarioMappper.toEdit(editar, atual);

        assertEquals("NovoNome", resultado.getNome());
        assertEquals("novo@email.com", resultado.getEmail());
        // Os demais campos devem permanecer do atual
        assertEquals(atual.getSobrenome(), resultado.getSobrenome());
    }

    // ── toEntity(UsuarioEditDTO) ───────────────────────────────────────────
    @Test
    @DisplayName("toEntity(EditDTO): retorna null para entrada null")
    void toEntity_editDtoNull_retornaNull() {
        assertNull(UsuarioMappper.toEntity((UsuarioEditDTO) null));
    }
}

