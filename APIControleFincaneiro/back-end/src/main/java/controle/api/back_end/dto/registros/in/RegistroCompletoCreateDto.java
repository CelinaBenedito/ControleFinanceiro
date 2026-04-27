package controle.api.back_end.dto.registros.in;

import jakarta.validation.Valid;

import java.util.List;

public class RegistroCompletoCreateDto {
    @Valid
    private EventoFinanceiroCreateDto financeiro;
    @Valid
    private List<EventoInstituicaoCreateDto> instituicao;
    @Valid
    private GastoDetalheCreateDto detalhe;

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
}
