package controle.api.back_end.dto.instituicao.mapper;

import controle.api.back_end.dto.instituicao.out.InstituicaoUsuarioResponseDTO;
import controle.api.back_end.model.instituicao.Instituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;

public class InstituicaoUsuarioMapper {

    public static InstituicaoUsuarioResponseDTO toDto(InstituicaoUsuario entity){
        if(entity==null){
            return null;
        }

        InstituicaoUsuarioResponseDTO dto = new InstituicaoUsuarioResponseDTO();
        InstituicaoUsuarioResponseDTO.UsuarioInstituicaoDTO entityUsuario = new InstituicaoUsuarioResponseDTO.UsuarioInstituicaoDTO();
        InstituicaoUsuarioResponseDTO.InstituicaoUsuarioDTO entityInstituicao = new InstituicaoUsuarioResponseDTO.InstituicaoUsuarioDTO();

        Usuario dtoUsuario = entity.getUsuario();
        Instituicao dtoInstituicao = entity.getInstituicao();

        entityUsuario.setId(dtoUsuario.getId());
        entityUsuario.setNome(dtoUsuario.getNome());
        entityUsuario.setSobrenome(dtoUsuario.getSobrenome());
        entityUsuario.setEmail(dtoUsuario.getEmail());
        entityUsuario.setGenero(dtoUsuario.getGenero());
        entityUsuario.setDataNascimento(dtoUsuario.getDataNascimento());

        entityInstituicao.setId(dtoInstituicao.getId());
        entityInstituicao.setNome(dtoInstituicao.getNome());
        entityInstituicao.setIsVoucher(dtoInstituicao.getIsVoucher());
        entityInstituicao.setLimiteCredito(entity.getLimiteCredito());
        entityInstituicao.setTaxaJuros(entity.getTaxaJuros());
        entityInstituicao.setTiposAceitos(entity.getTiposAceitos());
        entityInstituicao.setDiaVencimentoFatura(entity.getDiaVencimentoFatura());

        dto.setUltimaAtualizacao(entity.getUltimaModificacao());
        dto.setId(entity.getId());
        dto.setUsuario(entityUsuario);
        dto.setIntituicao(entityInstituicao);
        dto.setAtivo(entity.getIsAtivo());

        return dto;
    }
}
