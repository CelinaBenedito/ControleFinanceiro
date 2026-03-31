package controle.api.back_end.service;

import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.Instituicao;
import controle.api.back_end.repository.InstituicaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InstituicaoService {
    private final InstituicaoRepository instituicaoRepository;

    public InstituicaoService(InstituicaoRepository instituicaoRepository) {
        this.instituicaoRepository = instituicaoRepository;
    }

    public List<Instituicao> getInstituicoes() {
        return instituicaoRepository.findAll();
    }

    public Instituicao getInstituicaoById(Integer id) {
        return instituicaoRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                                "Instituicao de id: d% não encontrada".formatted(id)
                        )
                );
    }


    public Instituicao createInstituicao(Instituicao entity) {
        return instituicaoRepository.save(entity);
    }

    public void deleteInstituicao(Integer id) {
        if(instituicaoRepository.existsById(id)){
            instituicaoRepository.deleteInstituicaoById(id);

        }else {
            throw new EntidadeNaoEncontradaException("Instituição de id: %d não encontrada.".formatted(id));
        }
    }
}
