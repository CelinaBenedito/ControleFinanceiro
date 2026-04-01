package controle.api.back_end.dto.instituicao.mapper;

import controle.api.back_end.dto.instituicao.InstituicaoResponseDTO;
import controle.api.back_end.dto.instituicao.InstituicaoUsuarioResponseDTO;
import controle.api.back_end.model.Instituicao;
import controle.api.back_end.model.InstituicaoUsuario;
import controle.api.back_end.model.Usuario;

public class InstituicaoUsuarioMapper {

    public static InstituicaoUsuarioResponseDTO toDto(InstituicaoUsuario entity){
        if(entity==null){
            return null;
        }

        InstituicaoUsuarioResponseDTO dto = new InstituicaoUsuarioResponseDTO();
        InstituicaoUsuarioResponseDTO.UsuarioInstituicaoDTO entityUsuario = new InstituicaoUsuarioResponseDTO.UsuarioInstituicaoDTO();
        InstituicaoUsuarioResponseDTO.InstituicaoUsuarioDTO entityInstituicao = new InstituicaoUsuarioResponseDTO.InstituicaoUsuarioDTO();

        Usuario dtoUsuario = entity.getFkUsuario();
        Instituicao dtoInstituicao = entity.getFkInstituicao();

        entityUsuario.setId(dtoUsuario.getId());
        entityUsuario.setNome(dtoUsuario.getNome());
        entityUsuario.setSobrenome(dtoUsuario.getSobrenome());
        entityUsuario.setEmail(dtoUsuario.getEmail());
        entityUsuario.setSexo(dtoUsuario.getSexo());
        entityUsuario.setDataNascimento(dtoUsuario.getDataNascimento());

        entityInstituicao.setId(dtoInstituicao.getId());
        entityInstituicao.setNome(dtoInstituicao.getNome());

        dto.setId(entity.getId());
        dto.setUsuario(entityUsuario);
        dto.setIntituicao(entityInstituicao);

        return dto;
    }
}
