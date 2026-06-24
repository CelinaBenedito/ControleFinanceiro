package controle.api.back_end.dto.upload;

import controle.api.back_end.dto.registros.out.RegistroResponseDto;

import java.util.List;

public class ImportResultDto {

    private int totalImportados;
    private List<RegistroResponseDto> registrosImportados;
    private List<String> erros;

    public ImportResultDto(int totalImportados,
                           List<RegistroResponseDto> registrosImportados,
                           List<String> erros) {
        this.totalImportados = totalImportados;
        this.registrosImportados = registrosImportados;
        this.erros = erros;
    }

    public int getTotalImportados() {
        return totalImportados;
    }

    public void setTotalImportados(int totalImportados) {
        this.totalImportados = totalImportados;
    }

    public List<RegistroResponseDto> getRegistrosImportados() {
        return registrosImportados;
    }

    public void setRegistrosImportados(List<RegistroResponseDto> registrosImportados) {
        this.registrosImportados = registrosImportados;
    }

    public List<String> getErros() {
        return erros;
    }

    public void setErros(List<String> erros) {
        this.erros = erros;
    }
}

