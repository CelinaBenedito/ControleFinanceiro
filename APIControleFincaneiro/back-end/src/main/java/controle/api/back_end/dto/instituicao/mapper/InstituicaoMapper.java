package controle.api.back_end.dto.instituicao.mapper;

import controle.api.back_end.dto.instituicao.InstituicaoCreateDTO;
import controle.api.back_end.dto.instituicao.InstituicaoResponseDTO;
import controle.api.back_end.model.Instituicao;
import controle.api.back_end.model.InstituicaoUsuario;
import jakarta.validation.Valid;

import java.util.List;

public class InstituicaoMapper {

    public static InstituicaoResponseDTO toDto(Instituicao entity){
        if(entity==null){
            return null;
        }

        InstituicaoResponseDTO dto = new InstituicaoResponseDTO();

        dto.setId(entity.getId());
        dto.setNome(entity.getNome());

        return dto;
    }

    public static List<InstituicaoResponseDTO> toDto(List<Instituicao> entitys) {
        return entitys.stream()
                .map(InstituicaoMapper::toDto)
                .toList();
    }

    public static Instituicao toEntity(@Valid InstituicaoCreateDTO dto) {
        Instituicao entity = new Instituicao();

        entity.setNome(dto.getNome());
        return entity;
    }

    public static InstituicaoResponseDTO instituicaoUsuarioToDto(InstituicaoUsuario instituicoeUsuario) {
        if (instituicoeUsuario == null){
            return null;
        }
        InstituicaoResponseDTO response = new InstituicaoResponseDTO();

        Instituicao modelInstituicao = instituicoeUsuario.getFkInstituicao();

        response.setId(modelInstituicao.getId());
        response.setNome(modelInstituicao.getNome());

        return response;
    }

    public static List<InstituicaoResponseDTO> instituicaoUsuarioToDto(List<InstituicaoUsuario> models){
        return models.stream()
                .map(InstituicaoMapper::instituicaoUsuarioToDto)
                .toList();
    }
}
