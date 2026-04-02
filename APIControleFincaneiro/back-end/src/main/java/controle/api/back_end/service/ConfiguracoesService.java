package controle.api.back_end.service;

import controle.api.back_end.repository.ConfiguracoesRepository;

public class ConfiguracoesService {

    private final ConfiguracoesRepository configuracoesRepository;

    public ConfiguracoesService(ConfiguracoesRepository configuracoesRepository) {
        this.configuracoesRepository = configuracoesRepository;
    }
}
