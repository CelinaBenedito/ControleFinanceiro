package controle.api.back_end.dto.registros.mapper;

import controle.api.back_end.dto.registros.in.EventoFinanceiroCreateDto;
import controle.api.back_end.dto.registros.in.EventoInstituicaoCreateDto;
import controle.api.back_end.dto.registros.in.GastoDetalheCreateDto;
import controle.api.back_end.dto.registros.out.RegistroUsuarioResponseDto;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.GastoDetalhe;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import jakarta.validation.Valid;

public class RegistrosMapper {

    public static EventoFinanceiro toEntity(@Valid EventoFinanceiroCreateDto dto){
        if (dto == null){
            return null;
        }

        EventoFinanceiro entity = new EventoFinanceiro();
        Usuario user = new Usuario();
        user.setId(dto.getUsuario_id());

        entity.setFkUsuario(user);
        entity.setValor(dto.getValor());
        entity.setTipo(dto.getTipo());
        entity.setDescricao(dto.getDescricao());
        entity.setDataEvento(dto.getDataEvento());

        return entity;
    }

    public static EventoInstituicao toEntity(@Valid EventoInstituicaoCreateDto dto){
        if (dto == null){
            return null;
        }
        EventoInstituicao entity = new EventoInstituicao();
        InstituicaoUsuario instituicaoUsuario = new InstituicaoUsuario();
        instituicaoUsuario.setId(dto.getInstituicaoUsuario_id());

        entity.setValor(dto.getValor());
        entity.setTipoMovimento(dto.getTipoMovimento());
        entity.setInstituicaoUsuario(instituicaoUsuario);

        return entity;
    }

    public static GastoDetalhe toEntity(@Valid GastoDetalheCreateDto dto){
        if(dto == null){
            return null;
        }

        GastoDetalhe entity = new GastoDetalhe();
        CategoriaUsuario categoriaUsuario = new CategoriaUsuario();

        categoriaUsuario.setId(dto.getCategoriaUsuario_id());

        entity.setCategoriaUsuario(categoriaUsuario);
        entity.setTituloGasto(dto.getTituloGasto());

        return entity;
    }

    public static RegistroUsuarioResponseDto toResponseUser(EventoFinanceiro eventoFinanceiro, EventoInstituicao eventoInstituicao, GastoDetalhe gastoDetalhe){
        if(eventoInstituicao == null || eventoFinanceiro == null || gastoDetalhe == null){
            return null;
        }

        RegistroUsuarioResponseDto response = new RegistroUsuarioResponseDto();
        RegistroUsuarioResponseDto.EventoInstituicaoDto eventoInstituicaoDto = new RegistroUsuarioResponseDto.EventoInstituicaoDto();
        RegistroUsuarioResponseDto.EventoInstituicaoDto.InstituicaoDto instituicaoDto = new RegistroUsuarioResponseDto.EventoInstituicaoDto.InstituicaoDto();
        RegistroUsuarioResponseDto.EventoFinanceiroDto financeiroDto = new RegistroUsuarioResponseDto.EventoFinanceiroDto();
        RegistroUsuarioResponseDto.GastoDetalheDto detalheDto = new RegistroUsuarioResponseDto.GastoDetalheDto();
        RegistroUsuarioResponseDto.GastoDetalheDto.CategoriaDto categoriaDto = new RegistroUsuarioResponseDto.GastoDetalheDto.CategoriaDto();
        RegistroUsuarioResponseDto.UsuarioDto usuarioDto = new RegistroUsuarioResponseDto.UsuarioDto();

        instituicaoDto.setId(eventoInstituicao.getInstituicaoUsuario().getInstituicao().getId());
        instituicaoDto.setNome(eventoInstituicao.getInstituicaoUsuario().getInstituicao().getNome());

        eventoInstituicaoDto.setId(eventoInstituicao.getId());
        eventoInstituicaoDto.setInstituicao(instituicaoDto);
        eventoInstituicaoDto.setTipoMovimento(eventoInstituicao.getTipoMovimento());
        eventoInstituicaoDto.setValor(eventoInstituicao.getValor());

        financeiroDto.setId(eventoFinanceiro.getId());
        financeiroDto.setDescricao(eventoFinanceiro.getDescricao());
        financeiroDto.setTipo(eventoFinanceiro.getTipo());
        financeiroDto.setDataEvento(eventoFinanceiro.getDataEvento());
        financeiroDto.setValor(eventoFinanceiro.getValor());

        categoriaDto.setId(gastoDetalhe.getCategoriaUsuario().getCategoria().getId());
        categoriaDto.setTitulo(gastoDetalhe.getCategoriaUsuario().getCategoria().getTitulo());

        detalheDto.setId(gastoDetalhe.getId());
        detalheDto.setCategoria(categoriaDto);
        detalheDto.setTituloGasto(gastoDetalhe.getTituloGasto());


        usuarioDto.setId(eventoFinanceiro.getFkUsuario().getId());
        usuarioDto.setNome(eventoFinanceiro.getFkUsuario().getNome());
        usuarioDto.setEmail(eventoFinanceiro.getFkUsuario().getEmail());

        response.setUsuario(usuarioDto);
        response.setGastoDetalhe(detalheDto);
        response.setEventoFinanceiro(financeiroDto);
        response.setEventoInstituicao(eventoInstituicaoDto);
        return response;
    }
}
