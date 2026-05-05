package controle.api.back_end.dto.registros.in;

import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import jakarta.validation.Valid;

import java.util.List;

public class RegistroTransferenciaDTO {
    @Valid
    private EventoFinanceiroCreateDto financeiro;
    @Valid
    private List<EventoInstituicaoCreateDto> instituicao;
    @Valid
    private GastoDetalheCreateDto detalhe;
    private InstituicaoUsuario instituicaoRecebendo;

    public EventoFinanceiroCreateDto getFinanceiro() {
        return financeiro;
    }

    public void setFinanceiro(EventoFinanceiroCreateDto financeiro) {
        this.financeiro = financeiro;
    }

    public List<EventoInstituicaoCreateDto> getInstituicao() {
        return instituicao;
    }

    public void setInstituicao(List<EventoInstituicaoCreateDto> instituicao) {
        this.instituicao = instituicao;
    }

    public GastoDetalheCreateDto getDetalhe() {
        return detalhe;
    }

    public void setDetalhe(GastoDetalheCreateDto detalhe) {
        this.detalhe = detalhe;
    }

    public InstituicaoUsuario getInstituicaoRecebendo() {
        return instituicaoRecebendo;
    }

    public void setInstituicaoRecebendo(InstituicaoUsuario instituicaoRecebendo) {
        this.instituicaoRecebendo = instituicaoRecebendo;
    }
}
