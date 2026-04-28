function carregarRegistros() {
    const usuarioLogado = JSON.parse(localStorage.getItem("usuarioLogado") || "null");
    const userId = usuarioLogado ? usuarioLogado.id : null;
    if (!userId) return;

    MainAPI.carregarRegistros(userId)
        .then(json => {

            registros.innerHTML = "";

            const formatadorMoeda = new Intl.NumberFormat("pt-BR", {
                style: "currency",
                currency: "BRL"
            });

            const agrupado = {};

            json.forEach(registro => {
                const dataISO = registro.eventoFinanceiro && registro.eventoFinanceiro.dataEvento;
                if (!dataISO) return;
                const data = new Date(dataISO + 'T00:00:00');
                const ano = data.getFullYear();
                const mes = data.getMonth();

                if (!agrupado[ano]) agrupado[ano] = {};
                if (!agrupado[ano][mes]) agrupado[ano][mes] = [];

                agrupado[ano][mes].push(registro);
            });

            Object.keys(agrupado)
                .sort((a, b) => b - a)
                .forEach(ano => {

                    const anoDiv = document.createElement("div");
                    anoDiv.className = "ano-bloco";

                    anoDiv.innerHTML = `
                        <div class="ano-header">${ano}</div>
                        <div class="mes-container hidden"></div>
                    `;

                    const mesContainer = anoDiv.querySelector(".mes-container");

                    Object.keys(agrupado[ano])
                        .sort((a, b) => b - a)
                        .forEach(mes => {

                            const nomeMes = new Date(ano, mes)
                                .toLocaleString("pt-BR", { month: "long" });

                            const mesDiv = document.createElement("div");
                            mesDiv.className = "mes-bloco";

                            mesDiv.innerHTML = `
                                <div class="mes-header">${nomeMes}</div>
                                <div class="cards hidden"></div>
                            `;

                            const cardsDiv = mesDiv.querySelector(".cards");

                            agrupado[ano][mes]
                                .sort((a, b) => new Date(a.eventoFinanceiro.dataEvento) - new Date(b.eventoFinanceiro.dataEvento))
                                .forEach(registro => {

                                    const dataISO = registro.eventoFinanceiro.dataEvento;
                                    const data = new Date(dataISO + 'T00:00:00');
                                    const dia = String(data.getDate()).padStart(2, "0");
                                    const titulo = registro.gastoDetalhe ? registro.gastoDetalhe.tituloGasto : '-';
                                    const descricao = registro.eventoFinanceiro.descricao || '';
                                    const valor = registro.eventoFinanceiro.valor || 0;
                                    const tipo = registro.eventoFinanceiro.tipo || '';
                                    const instNome = registro.eventoInstituicao && registro.eventoInstituicao[0] && registro.eventoInstituicao[0].instituicao
                                        ? registro.eventoInstituicao[0].instituicao.nome : '-';

                                    const registroId = registro.eventoFinanceiro.id || '';
                                    const registroData = {
                                        id: registroId,
                                        titulo,
                                        descricao,
                                        valor,
                                        tipo,
                                        instituicao: instNome,
                                        data: dataISO
                                    };

                                    const card = document.createElement('div');
                                    card.className = 'cardRegistro';
                                    card.innerHTML = `
                                        <div class="dataRegistro">${dia}</div>

                                        <div class="registroInfo">
                                            <div class="registroTitulo">${titulo}</div>
                                            <div class="registroDescricao">${descricao}</div>
                                        </div>

                                        <div class="registroDetalhes">
                                            <div class="registroValor">
                                                ${formatadorMoeda.format(valor)}
                                            </div>
                                            <div class="registroTipo">${tipo}</div>
                                            <div class="registroInstituicao">${instNome}</div>
                                        </div>

                                        <div class="registroAcoes">
                                            <button class="reg-icon-btn edit" title="Editar registro"><i class='bx bx-edit'></i></button>
                                            <button class="reg-icon-btn delete" title="Remover registro"><i class='bx bx-trash'></i></button>
                                        </div>
                                    `;

                                    card.querySelector('.reg-icon-btn.edit').addEventListener('click', () => abrirEdicaoRegistro(registroData));
                                    card.querySelector('.reg-icon-btn.delete').addEventListener('click', () => confirmarRemocaoRegistro(registroData));

                                    cardsDiv.appendChild(card);
                                });

                            mesContainer.appendChild(mesDiv);
                        });

                    registros.appendChild(anoDiv);
                });

            // 🔹 Eventos de clique
            document.querySelectorAll(".ano-header").forEach(el => {
                el.addEventListener("click", () => {
                    el.classList.toggle("open");
                    el.nextElementSibling.classList.toggle("hidden");
                });
            });

            document.querySelectorAll(".mes-header").forEach(el => {
                el.addEventListener("click", () => {
                    el.classList.toggle("open");
                    el.nextElementSibling.classList.toggle("hidden");
                });
            });

        });


}



function adicionar() {
    var valor = ipt_valor.value;
    var instituicao = select_instituicao.value;
    MainAPI.adicionarSaldo({
        valorServer: valor,
        instituicaoServer: instituicao
    }).then((resposta) => {
        console.log("Resposta:", resposta);
        if (resposta.ok) {
            console.log("Saldo atualizado")
            return window.location.reload()
        }
        else {
            console.error("Houver um erro ao atualizar o saldo", resposta)
        }
    });
}
function remover() {
    var valor = ipt_remove.value;
    var instituicao = select_instituicao_remove.value;
    MainAPI.atualizarSaldo({
        valorServer: valor,
        instituicaoServer: instituicao
    }).then((resposta) => {
        console.log("Resposta:", resposta);
        if (resposta.ok) {
            console.log("Saldo atualizado")
            return window.location.reload()
        }
        else {
            console.error("Houver um erro ao atualizar o saldo", resposta)
        }
    });
}

function gerarInstituicao() {
    select_instituicao.innerHTML = "<option value='#'> Escolha uma instituição</option>"
    MainAPI.getInstituicoes().then(json => {
        for (let c = 0; json.length > c; c++) {
            select_instituicao_remove.innerHTML +=
                `
                            <option value="${json[c].id}">${json[c].nome}</option>
                    `
            select_instituicao.innerHTML +=
                `
                            <option value="${json[c].id}">${json[c].nome}</option>
                        `
        }
    })
}
MainAPI.mostrarSaldoTotal().then(json => {
    ValorTotal.innerHTML = json[0].valorTotal
})

function Consulta() {
    principal.innerHTML = "    "
    MainAPI.mostrarTodasInstituicoes().then(json => {
        for (let c = 0; json.length > c; c++) {
            principal.innerHTML +=
                `<div>
                            <h1>${json[c].nome}</h1>
                            <div class="textoValor">
                                <p>${json[c].valor}</p>
                            </div>
                            </div>
                        `
        }
    })
}

// ── EDITAR REGISTRO ──────────────────────────────────────────────────────────
function abrirEdicaoRegistro(registro) {
    let modal = document.getElementById('modalEdicaoRegistro');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'modalEdicaoRegistro';
        modal.style.cssText = `
            position:fixed;inset:0;background:rgba(0,0,0,0.5);
            display:flex;align-items:center;justify-content:center;z-index:9999;
        `;
        modal.innerHTML = `
            <div style="background:#FAFFFF;border-radius:20px;padding:32px;width:min(480px,92vw);
                        display:flex;flex-direction:column;gap:16px;box-shadow:0 8px 32px rgba(0,0,0,0.25);">
                <h2 style="color:#004C58;margin:0;font-size:1.4rem;font-family:'Inter',sans-serif;">Editar Registro</h2>

                <div class="er-field-wrap">
                    <input id="erTitulo" type="text" placeholder=" ">
                    <label for="erTitulo">Título</label>
                </div>

                <div class="er-field-wrap">
                    <input id="erDescricao" type="text" placeholder=" ">
                    <label for="erDescricao">Descrição</label>
                </div>

                <div class="er-field-wrap">
                    <input id="erValor" type="number" placeholder=" ">
                    <label for="erValor">Valor</label>
                </div>

                <div class="er-field-wrap" style="position:relative;">
                    <select id="erMovimento">
                        <option value="Débito">Débito</option>
                        <option value="Crédito">Crédito</option>
                        <option value="Transferência">Transferência</option>
                    </select>
                    <label for="erMovimento">Movimento</label>
                    <span style="position:absolute;right:14px;top:50%;transform:translateY(-50%);color:#367373;pointer-events:none;">▾</span>
                </div>

                <div class="er-field-wrap">
                    <input id="erData" type="date" placeholder=" ">
                    <label for="erData">Data</label>
                </div>

                <div style="display:flex;gap:12px;margin-top:4px;">
                    <button onclick="document.getElementById('modalEdicaoRegistro').style.display='none'"
                        style="flex:1;height:52px;background:transparent;color:#367373;border:2px solid #367373;
                               border-radius:10px;font-size:1rem;font-family:'Inter',sans-serif;cursor:pointer;transition:background 0.2s;">
                        Cancelar
                    </button>
                    <button id="erBtnSalvar"
                        style="flex:1;height:52px;background:#367373;color:#fff;border:none;border-radius:10px;
                               font-size:1rem;font-family:'Inter',sans-serif;cursor:pointer;transition:background 0.2s;">
                        Salvar
                    </button>
                </div>
            </div>
        `;

        // Fecha ao clicar fora do card
        modal.addEventListener('click', e => {
            if (e.target === modal) modal.style.display = 'none';
        });

        // Hover nos botões
        modal.querySelector('#erBtnSalvar').addEventListener('mouseover', e => e.target.style.background = '#004C58');
        modal.querySelector('#erBtnSalvar').addEventListener('mouseout', e => e.target.style.background = '#367373');

        document.body.appendChild(modal);
    }

    // Preenche com dados do registro
    modal.querySelector('#erTitulo').value = registro.titulo || '';
    modal.querySelector('#erDescricao').value = registro.descricao || '';
    modal.querySelector('#erValor').value = registro.valor || '';
    modal.querySelector('#erMovimento').value = registro.tipo || 'Débito';
    modal.querySelector('#erData').value = registro.data || '';

    // Guarda o id para uso futuro na integração
    modal.dataset.registroId = registro.id;

    // Botão Salvar — preparado para integração: substituir TODO pelo call de API
    modal.querySelector('#erBtnSalvar').onclick = () => {
        // TODO: integrar com MainAPI.editarRegistro(...)
        console.log('Salvar edição do registro id:', modal.dataset.registroId, {
            titulo: modal.querySelector('#erTitulo').value,
            descricao: modal.querySelector('#erDescricao').value,
            valor: modal.querySelector('#erValor').value,
            movimento: modal.querySelector('#erMovimento').value,
            data: modal.querySelector('#erData').value,
        });
        modal.style.display = 'none';
    };

    modal.style.display = 'flex';
}

// ── CONFIRMAR REMOÇÃO ────────────────────────────────────────────────────────
function confirmarRemocaoRegistro(registro) {
    let popup = document.getElementById('popupConfirmarRemocao');
    if (!popup) {
        popup = document.createElement('div');
        popup.id = 'popupConfirmarRemocao';
        popup.style.cssText = `
            position:fixed;inset:0;background:rgba(0,0,0,0.5);
            display:flex;align-items:center;justify-content:center;z-index:9999;
        `;
        popup.innerHTML = `
            <div style="background:#FAFFFF;border-radius:20px;padding:32px;width:min(380px,90vw);
                        display:flex;flex-direction:column;gap:20px;box-shadow:0 8px 32px rgba(0,0,0,0.25);
                        font-family:'Inter',sans-serif;align-items:center;text-align:center;">
                <i class='bx bx-error-circle' style="font-size:3rem;color:#e53e3e;"></i>
                <div>
                    <p style="font-size:1.1rem;font-weight:600;color:#1A1A1A;margin:0 0 6px;">Remover registro?</p>
                    <p id="popupRemocaoNome" style="font-size:0.95rem;color:#4A4A4A;margin:0;"></p>
                </div>
                <p style="font-size:0.85rem;color:#888;margin:0;">Esta ação não pode ser desfeita.</p>
                <div style="display:flex;gap:12px;width:100%;">
                    <button onclick="document.getElementById('popupConfirmarRemocao').style.display='none'"
                        style="flex:1;height:48px;background:transparent;color:#367373;border:2px solid #367373;
                               border-radius:10px;font-size:1rem;cursor:pointer;transition:background 0.2s;">
                        Cancelar
                    </button>
                    <button id="popupBtnConfirmar"
                        style="flex:1;height:48px;background:#e53e3e;color:#fff;border:none;border-radius:10px;
                               font-size:1rem;cursor:pointer;transition:background 0.2s;">
                        Remover
                    </button>
                </div>
            </div>
        `;

        // Fecha ao clicar fora
        popup.addEventListener('click', e => {
            if (e.target === popup) popup.style.display = 'none';
        });

        popup.querySelector('#popupBtnConfirmar').addEventListener('mouseover', e => e.target.style.background = '#c53030');
        popup.querySelector('#popupBtnConfirmar').addEventListener('mouseout', e => e.target.style.background = '#e53e3e');

        document.body.appendChild(popup);
    }

    popup.querySelector('#popupRemocaoNome').textContent = registro.titulo ? `"${registro.titulo}"` : '';
    popup.dataset.registroId = registro.id;

    // Botão Confirmar — preparado para integração
    popup.querySelector('#popupBtnConfirmar').onclick = () => {
        // TODO: integrar com MainAPI.removerRegistro(popup.dataset.registroId)
        console.log('Remover registro id:', popup.dataset.registroId);
        popup.style.display = 'none';
    };

    popup.style.display = 'flex';
}