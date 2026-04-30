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

                                    card.querySelector('.reg-icon-btn.edit').addEventListener('click', () => abrirEdicaoRegistro(registro));
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
async function abrirEdicaoRegistro(registro) {
    const usuarioLogado = JSON.parse(localStorage.getItem("usuarioLogado") || "null");
    const userId = usuarioLogado ? usuarioLogado.id : null;
    if (!userId) return;

    const ef = registro.eventoFinanceiro || {};
    const ei = (registro.eventoInstituicao && registro.eventoInstituicao[0]) || {};
    const gd = registro.gastoDetalhe || {};

    // Carregar instituições e categorias do usuário em paralelo
    let instList = [], catList = [];
    try {
        const [instRes, catRes] = await Promise.all([
            fetch(`http://localhost:8080/instituicoes/usuarios/${userId}`),
            fetch(`http://localhost:8080/categorias/usuario/${userId}`)
        ]);
        instList = (instRes.ok && instRes.status !== 204) ? await instRes.json() : [];
        catList  = (catRes.ok  && catRes.status  !== 204) ? await catRes.json()  : [];
    } catch (e) {
        console.error('Erro ao carregar dados para edição:', e);
    }

    const instAtualId = ei.instituicao ? ei.instituicao.id : null;
    const catAtualId  = (gd.categoria && gd.categoria[0]) ? gd.categoria[0].id : null;

    const instOpts = instList.map(i =>
        `<option value="${i.id}"${i.intituicao.id === instAtualId ? ' selected' : ''}>${i.intituicao.nome}</option>`
    ).join('');

    const catOpts = catList.map(c =>
        `<option value="${c.id}"${c.categoria.id === catAtualId ? ' selected' : ''}>${c.categoria.titulo}</option>`
    ).join('');

    const mostrarParcelas = ei.tipoMovimento === 'Credito' || ei.tipoMovimento === 'Boleto';

    // Remove modal anterior se existir (para sempre refletir dados atualizados)
    const anterior = document.getElementById('modalEdicaoRegistro');
    if (anterior) anterior.remove();

    const modal = document.createElement('div');
    modal.id = 'modalEdicaoRegistro';
    modal.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:9999;';
    modal.innerHTML = `
        <div style="background:#FAFFFF;border-radius:20px;padding:32px;width:min(520px,92vw);
                    display:flex;flex-direction:column;gap:16px;box-shadow:0 8px 32px rgba(0,0,0,0.25);
                    max-height:90vh;overflow-y:auto;font-family:'Inter',sans-serif;">
            <h2 style="color:#004C58;margin:0;font-size:1.4rem;">Editar Registro</h2>

            <div class="er-field-wrap">
                <input id="erTitulo" type="text" placeholder=" " value="${(gd.tituloGasto || '').replace(/"/g, '&quot;')}">
                <label for="erTitulo">Título</label>
            </div>

            <div class="er-field-wrap">
                <input id="erDescricao" type="text" placeholder=" " value="${(ef.descricao || '').replace(/"/g, '&quot;')}">
                <label for="erDescricao">Descrição</label>
            </div>

            <div class="er-field-wrap" style="position:relative;">
                <select id="erTipo">
                    <option value="Gasto"${ef.tipo === 'Gasto' ? ' selected' : ''}>Gasto</option>
                    <option value="Recebimento"${ef.tipo === 'Recebimento' ? ' selected' : ''}>Recebimento</option>
                    <option value="Transferencia"${ef.tipo === 'Transferencia' ? ' selected' : ''}>Transferência</option>
                </select>
                <label for="erTipo">Tipo do Evento</label>
                <span style="position:absolute;right:14px;top:50%;transform:translateY(-50%);color:#367373;pointer-events:none;">▾</span>
            </div>

            <div class="er-field-wrap">
                <input id="erValor" type="number" placeholder=" " value="${ef.valor || ''}">
                <label for="erValor">Valor</label>
            </div>

            <div class="er-field-wrap" style="position:relative;">
                <select id="erMovimento" onchange="var p=document.getElementById('erWrapParcelas');if(this.value==='Credito'||this.value==='Boleto'){p.style.display='';}else{p.style.display='none';document.getElementById('erParcelas').value=1;}">
                    <option value="Debito"${ei.tipoMovimento === 'Debito' ? ' selected' : ''}>Débito</option>
                    <option value="Credito"${ei.tipoMovimento === 'Credito' ? ' selected' : ''}>Crédito</option>
                    <option value="Dinheiro"${ei.tipoMovimento === 'Dinheiro' ? ' selected' : ''}>Dinheiro</option>
                    <option value="Pix"${ei.tipoMovimento === 'Pix' ? ' selected' : ''}>Pix</option>
                    <option value="Boleto"${ei.tipoMovimento === 'Boleto' ? ' selected' : ''}>Boleto</option>
                    <option value="Voucher"${ei.tipoMovimento === 'Voucher' ? ' selected' : ''}>Voucher</option>
                </select>
                <label for="erMovimento">Movimento</label>
                <span style="position:absolute;right:14px;top:50%;transform:translateY(-50%);color:#367373;pointer-events:none;">▾</span>
            </div>

            <div id="erWrapParcelas" class="er-field-wrap" style="${mostrarParcelas ? '' : 'display:none;'}">
                <input id="erParcelas" type="number" min="1" placeholder=" " value="${ei.parcelas || 1}">
                <label for="erParcelas">Parcelas</label>
            </div>

            <div class="er-field-wrap" style="position:relative;">
                <select id="erInstituicao">${instOpts}</select>
                <label for="erInstituicao">Instituição</label>
                <span style="position:absolute;right:14px;top:50%;transform:translateY(-50%);color:#367373;pointer-events:none;">▾</span>
            </div>

            <div class="er-field-wrap" style="position:relative;">
                <select id="erCategoria">${catOpts}</select>
                <label for="erCategoria">Categoria</label>
                <span style="position:absolute;right:14px;top:50%;transform:translateY(-50%);color:#367373;pointer-events:none;">▾</span>
            </div>

            <div class="er-field-wrap">
                <input id="erData" type="date" placeholder=" " value="${ef.dataEvento || ''}">
                <label for="erData">Data</label>
            </div>

            <p id="erMsgErro" style="color:#e53e3e;font-size:0.9rem;margin:0;display:none;"></p>

            <div style="display:flex;gap:12px;">
                <button id="erBtnCancelar"
                    style="flex:1;height:52px;background:transparent;color:#367373;border:2px solid #367373;
                           border-radius:10px;font-size:1rem;cursor:pointer;">
                    Cancelar
                </button>
                <button id="erBtnSalvar"
                    style="flex:1;height:52px;background:#367373;color:#fff;border:none;
                           border-radius:10px;font-size:1rem;cursor:pointer;">
                    Salvar
                </button>
            </div>
        </div>
    `;

    modal.addEventListener('click', e => { if (e.target === modal) modal.remove(); });
    modal.querySelector('#erBtnCancelar').addEventListener('click', () => modal.remove());

    modal.querySelector('#erBtnSalvar').addEventListener('click', async () => {
        const titulo    = modal.querySelector('#erTitulo').value.trim();
        const descricao = modal.querySelector('#erDescricao').value.trim() || 'Nenhuma descrição fornecida';
        const tipo      = modal.querySelector('#erTipo').value;
        const valor     = Number(modal.querySelector('#erValor').value);
        const movimento = modal.querySelector('#erMovimento').value;
        const parcelas  = Number(modal.querySelector('#erParcelas').value) || 1;
        const instId    = Number(modal.querySelector('#erInstituicao').value);
        const catId     = Number(modal.querySelector('#erCategoria').value);
        const data      = modal.querySelector('#erData').value;
        const msgErro   = modal.querySelector('#erMsgErro');
        const btn       = modal.querySelector('#erBtnSalvar');

        if (!titulo)                 { msgErro.textContent = 'Título obrigatório.'; msgErro.style.display = ''; return; }
        if (valor <= 0 || isNaN(valor)) { msgErro.textContent = 'Valor inválido.';  msgErro.style.display = ''; return; }
        if (!data)                   { msgErro.textContent = 'Data obrigatória.';  msgErro.style.display = ''; return; }
        msgErro.style.display = 'none';

        btn.disabled = true;
        btn.textContent = 'Salvando...';

        const payload = {
            financeiro:  { usuario_id: userId, tipo, valor, descricao, dataEvento: data },
            instituicao: [{ instituicaoUsuario_id: instId, tipoMovimento: movimento, valor, parcelas }],
            detalhe:     { categoriaUsuario_id: [catId], tituloGasto: titulo }
        };

        try {
            const res = await MainAPI.editarRegistro(ef.id, payload);
            if (res.ok) {
                modal.remove();
                carregarRegistros();
            } else {
                let detalhe = `HTTP ${res.status}`;
                try { const corpo = await res.json(); detalhe = corpo.message || JSON.stringify(corpo); } catch (_) {}
                msgErro.textContent = `Erro ao salvar: ${detalhe}`;
                msgErro.style.display = '';
                btn.disabled = false;
                btn.textContent = 'Salvar';
            }
        } catch (e) {
            msgErro.textContent = 'Erro de conexão.';
            msgErro.style.display = '';
            btn.disabled = false;
            btn.textContent = 'Salvar';
        }
    });

    document.body.appendChild(modal);
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
    popup.querySelector('#popupBtnConfirmar').onclick = async () => {
        const btn = popup.querySelector('#popupBtnConfirmar');
        btn.disabled = true;
        btn.textContent = 'Removendo...';
        try {
            const res = await MainAPI.deletarRegistro(popup.dataset.registroId);
            if (res.ok) {
                popup.style.display = 'none';
                carregarRegistros();
            } else {
                btn.disabled = false;
                btn.textContent = 'Remover';
            }
        } catch (e) {
            console.error('Erro ao remover registro:', e);
            btn.disabled = false;
            btn.textContent = 'Remover';
        }
    };

    popup.style.display = 'flex';
}