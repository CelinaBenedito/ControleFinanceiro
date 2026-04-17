package controle.api.back_end.dto.registros.in;

import jakarta.validation.Valid;

public class RegistroCompletoCreateDto {
    @Valid
    private EventoInstituicaoCreateDto instituicao;
    @Valid
    private EventoFinanceiroCreateDto financeiro;
    @Valid
    private GastoDetalheCreateDto detalhe;

    public EventoInstituicaoCreateDto getInstituicao() {
        return instituicao;
    }

    public void setInstituicao(EventoInstituicaoCreateDto instituicao) {
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
