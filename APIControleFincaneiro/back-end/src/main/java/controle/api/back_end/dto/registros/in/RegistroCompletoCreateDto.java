package controle.api.back_end.dto.registros.in;

import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.util.List;

public class RegistroCompletoCreateDto {
    @Valid
    private EventoFinanceiroCreateDto financeiro;
    @Valid
    private List<EventoInstituicaoCreateDto> instituicao;
    @Valid
    private GastoDetalheCreateDto detalhe;
    @Schema
    private Integer instituicaoRecebendo_id;


    public List<EventoInstituicaoCreateDto> getInstituicao() {
        return instituicao;
    }

    public void setInstituicao(List<EventoInstituicaoCreateDto> instituicao) {
        this.instituicao = instituicao;
    }

    public EventoFinanceiroCreateDto getFinanceiro() {
        return financeiro;
    }

    public void setFinanceiro(EventoFinanceiroCreateDto financeiro) {
        this.financeiro = financeiro;
    }

    public GastoDetalheCreateDto getDetalhe() {
        return detalhe;
    }

    public void setDetalhe(GastoDetalheCreateDto detalhe) {
        this.detalhe = detalhe;
    }

    public Integer getInstituicaoRecebendo_id() {
        return instituicaoRecebendo_id;
    }

    public void setInstituicaoRecebendo_id(Integer instituicaoRecebendo_id) {
        this.instituicaoRecebendo_id = instituicaoRecebendo_id;
    }
}
