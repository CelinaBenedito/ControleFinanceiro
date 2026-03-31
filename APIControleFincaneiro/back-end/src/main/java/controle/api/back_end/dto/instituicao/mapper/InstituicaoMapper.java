package controle.api.back_end.dto.instituicao.mapper;

import controle.api.back_end.dto.instituicao.InstituicaoCreateDTO;
import controle.api.back_end.dto.instituicao.InstituicaoResponseDTO;
import controle.api.back_end.model.Instituicao;
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
}
