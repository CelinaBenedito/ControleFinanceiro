function getUsuarioLogadoId() {
    const usuarioLogado = JSON.parse(localStorage.getItem("usuarioLogado") || "null");
    return usuarioLogado ? usuarioLogado.id : null;
}

function carregarRegistros() {
    const userId = getUsuarioLogadoId();
    if (!userId) return;

    MainAPI.carregarRegistros(userId)
        .then(renderizarRegistros)
        .catch(err => {
            console.error("Erro ao carregar registros:", err);
            registros.innerHTML = `<div class="aviso"><i class='bx bx-error-circle'></i><p>Erro ao carregar registros.</p></div>`;
        });

    inicializarBotaoFiltro();
}

function renderizarRegistros(json) {
    registros.innerHTML = "";

    if (!Array.isArray(json) || json.length === 0) {
        registros.innerHTML = `<div class="aviso"><i class='bx bx-search-alt'></i><p>Nenhum registro encontrado para o filtro aplicado.</p></div>`;
        return;
    }

    const formatadorMoeda = new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL"
    });

    const agrupado = {};

    json.forEach(registro => {
        const dataISO = registro.eventoFinanceiro && registro.eventoFinanceiro.dataEvento;
        if (!dataISO) return;
        const data = new Date(dataISO + "T00:00:00");
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
                    const nomeMes = new Date(ano, mes).toLocaleString("pt-BR", { month: "long" });
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
                            const data = new Date(dataISO + "T00:00:00");
                            const dia = String(data.getDate()).padStart(2, "0");
                            const titulo = registro.gastoDetalhe ? registro.gastoDetalhe.tituloGasto : "-";
                            const descricao = registro.eventoFinanceiro.descricao || "";
                            const valor = registro.eventoFinanceiro.valor || 0;
                            const tipo = registro.eventoFinanceiro.tipo || "";
                            const instNome = registro.eventoInstituicao && registro.eventoInstituicao[0] && registro.eventoInstituicao[0].instituicao
                                ? registro.eventoInstituicao[0].instituicao.nome : "-";

                            const registroId = registro.eventoFinanceiro.id || "";
                            const registroData = {
                                id: registroId,
                                titulo,
                                descricao,
                                valor,
                                tipo,
                                instituicao: instNome,
                                data: dataISO
                            };

                            const card = document.createElement("div");
                            card.className = "cardRegistro";
                            card.innerHTML = `
                                <div class="dataRegistro">${dia}</div>

                                <div class="registroInfo">
                                    <div class="registroTitulo">${titulo}</div>
                                    <div class="registroDescricao">${descricao}</div>
                                </div>

                                <div class="registroDetalhes">
                                    <div class="registroValor">${formatadorMoeda.format(valor)}</div>
                                    <div class="registroTipo">${tipo}</div>
                                    <div class="registroInstituicao">${instNome}</div>
                                </div>

                                <div class="registroAcoes">
                                    <button class="reg-icon-btn edit" title="Editar registro"><i class='bx bx-edit'></i></button>
                                    <button class="reg-icon-btn delete" title="Remover registro"><i class='bx bx-trash'></i></button>
                                </div>
                            `;

                            card.querySelector(".reg-icon-btn.edit").addEventListener("click", () => abrirEdicaoRegistro(registro));
                            card.querySelector(".reg-icon-btn.delete").addEventListener("click", () => confirmarRemocaoRegistro(registroData));

                            cardsDiv.appendChild(card);
                        });

                    mesContainer.appendChild(mesDiv);
                });

            registros.appendChild(anoDiv);
        });

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

function inicializarBotaoFiltro() {
    const btn = document.getElementById("btnAbrirFiltro");
    if (!btn || btn.dataset.bound === "1") return;
    btn.dataset.bound = "1";
    btn.addEventListener("click", abrirModalFiltroRegistros);
}

async function abrirModalFiltroRegistros() {
    const userId = getUsuarioLogadoId();
    if (!userId) return;

    const anterior = document.getElementById("modalFiltroRegistros");
    if (anterior) anterior.remove();

    let instituicoes = [];
    let categorias = [];
    try {
        [instituicoes, categorias] = await Promise.all([
            MainAPI.getInstituicoes(userId),
            MainAPI.getTipos(userId)
        ]);
    } catch (e) {
        console.error("Erro ao carregar opções do filtro:", e);
    }

    const modal = document.createElement("div");
    modal.id = "modalFiltroRegistros";
    modal.style.cssText = "position:fixed;inset:0;background:rgba(0,0,0,0.45);display:flex;align-items:center;justify-content:center;z-index:9999;padding:16px;";

    const opcoesTipo = ["Gasto", "Recebimento", "Transferencia"];
    const opcoesMovimento = ["Debito", "Credito", "Dinheiro", "Pix", "Boleto", "Voucher"];

    modal.innerHTML = `
        <style>
            .fr-card { width:min(1040px,96vw); max-height:90vh; overflow:auto; padding:28px; background:#FAFFFF; border-radius:30px; border:1px solid #CFE5E5; box-shadow:0 16px 42px rgba(14,84,84,0.22); display:flex; flex-direction:column; gap:24px; font-family:Inter,sans-serif; }
            .fr-top { display:flex; justify-content:space-between; align-items:center; gap:12px; }
            .fr-title { margin:0; color:#004C58; font-size:1.9rem; font-weight:800; letter-spacing:0.2px; }
            .fr-close { width:40px; height:40px; border:1px solid #9cc9c9; border-radius:10px; background:#ffffff; font-size:1.6rem; color:#367373; cursor:pointer; line-height:1; transition:.2s; }
            .fr-close:hover { background:#EAF6F6; border-color:#367373; }
            .fr-secao-titulo { margin:0 0 10px; color:#004C58; font-size:1.05rem; font-weight:700; }
            .fr-box { border:none; border-radius:0; padding:4px 0; max-height:235px; overflow:auto; background:transparent; }
            .fr-box::-webkit-scrollbar { width:10px; }
            .fr-box::-webkit-scrollbar-thumb { background:#B8D9D9; border-radius:8px; border:2px solid #fff; }
            .fr-grid-2 { display:flex; flex-direction:column; gap:18px; }
            .fr-lista { display:flex; flex-direction:column; gap:8px; }
            .fr-check { display:flex; align-items:center; gap:10px; font-size:1rem; color:#1f2b2b; padding:6px 8px; border-radius:8px; transition:background .15s; }
            .fr-check:hover { background:#F0F9F9; }
            .fr-check input { width:18px; height:18px; accent-color:#367373; }
            .fr-date-row { display:flex; align-items:center; gap:12px; flex-wrap:wrap; }
            .fr-date-display { font-size:1rem; font-weight:700; color:#0E5454; margin:0; padding:10px 12px; background:#F0F9F9; border:1px solid #BFDDDD; border-radius:10px; }
            .fr-btn { display:inline-flex; align-items:center; justify-content:center; height:50px; padding:0 18px; border-radius:10px; font-size:1rem; font-weight:700; cursor:pointer; transition:.2s; }
            .fr-btn.primary { background:#367373; color:#fff; border:none; box-shadow:0 6px 18px rgba(54,115,115,.26); }
            .fr-btn.primary:hover { background:#0E5454; }
            .fr-btn.secondary { background:transparent; color:#367373; border:2px solid #367373; }
            .fr-btn.secondary:hover { background:#EAF6F6; }
            .fr-actions { display:flex; justify-content:flex-end; gap:10px; }
            .fr-busca-grid { display:flex; gap:12px; align-items:end; }
            .fr-busca-grow { flex:1; min-width:220px; }
            .fr-select-wrap { position:relative; }
            .fr-select-wrap::after { content:'▾'; position:absolute; right:14px; top:50%; transform:translateY(-44%); color:#367373; pointer-events:none; font-size:.9rem; }
            .fr-select {
                width:100%; height:58px; border-radius:10px; border:2px solid #367373; padding:0 38px 0 14px; font-size:1rem; font-weight:600;
                color:#367373; background:#FAFFFF; appearance:none; outline:none; transition:border-color .2s, box-shadow .2s;
            }
            .fr-select:focus { border-color:#004C58; box-shadow:0 0 0 3px rgba(54,115,115,.15); }

            body.dark-mode #modalFiltroRegistros .fr-card { background:#0f172a; border-color:#24455c; box-shadow:0 14px 32px rgba(0,0,0,.55); }
            body.dark-mode #modalFiltroRegistros .fr-title,
            body.dark-mode #modalFiltroRegistros .fr-secao-titulo { color:#7dd3fc; }
            body.dark-mode #modalFiltroRegistros .fr-box { background:transparent; border:none; }
            body.dark-mode #modalFiltroRegistros .fr-check { color:#d6f0ff; }
            body.dark-mode #modalFiltroRegistros .fr-check:hover { background:#13243a; }
            body.dark-mode #modalFiltroRegistros .fr-date-display { background:#13243a; border-color:#31556c; color:#b9e8ff; }
            body.dark-mode #modalFiltroRegistros .fr-close { background:#13243a; border-color:#31556c; color:#9bdfff; }
            body.dark-mode #modalFiltroRegistros .fr-close:hover { background:#1a3050; }
            body.dark-mode #modalFiltroRegistros .fr-btn.secondary { color:#9bdfff; border-color:#5ca7d1; }
            body.dark-mode #modalFiltroRegistros .fr-btn.secondary:hover { background:#1a3050; }
            body.dark-mode #modalFiltroRegistros .fr-select { background:#0f172a; color:#9bdfff; border-color:#5ca7d1; }
            body.dark-mode #modalFiltroRegistros .fr-select-wrap::after { color:#9bdfff; }

            @media (max-width: 760px) {
                .fr-card { width:96vw; padding:16px; border-radius:20px; gap:16px; }
                .fr-title { font-size:1.45rem; }
                .fr-busca-grid { width:100%; flex-direction:column; align-items:stretch; }
                .fr-box { max-height:190px; }
                .fr-actions { flex-direction:column-reverse; }
                .fr-actions .fr-btn { width:100%; }
                .fr-date-row { align-items:stretch; }
            }
        </style>

        <div class="fr-card">
            <div class="fr-top">
                <h2 class="fr-title">Filtro de Registros</h2>
                <button id="frFechar" class="fr-close">&times;</button>
            </div>

            <div class="fr-busca-grid">
                <div class="campo fr-busca-grow">
                    <input id="frTextoBusca" type="text" placeholder=" ">
                    <label for="frTextoBusca">Digite para filtrar</label>
                </div>
                <div class="fr-select-wrap" style="width:320px;max-width:42%;min-width:220px;">
                    <select id="frCampoTexto" class="fr-select">
                        <option value="titulo">Filtrar por Título</option>
                        <option value="descricao">Filtrar por Descrição</option>
                    </select>
                </div>
            </div>

            <div class="fr-grid-2">
                <div>
                    <h4 class="fr-secao-titulo">Tipo de Evento Financeiro</h4>
                    <div id="frTipos" class="fr-box fr-lista"></div>
                </div>
                <div>
                    <h4 class="fr-secao-titulo">Tipo de Movimento</h4>
                    <div id="frMovimentos" class="fr-box fr-lista"></div>
                </div>
            </div>

            <div>
                <h4 class="fr-secao-titulo">Data do Evento</h4>
                <div class="fr-date-row" style="margin-top:8px;">
                    <p class="fr-date-display" id="frDataDisplay">Nenhuma data selecionada</p>
                    <button class="fr-btn secondary" id="frBtnData" type="button">
                        <i class='bx bx-calendar' style="margin-right:6px;"></i> Escolher data
                    </button>
                    <input id="frDataEvento" type="date" style="position:absolute;left:-9999px;opacity:0;pointer-events:none;" aria-hidden="true">
                </div>
            </div>

            <div class="fr-grid-2">
                <div>
                    <h4 class="fr-secao-titulo">Instituição</h4>
                    <div id="frInstituicoes" class="fr-box fr-lista"></div>
                </div>
                <div>
                    <h4 class="fr-secao-titulo">Categorias</h4>
                    <div id="frCategorias" class="fr-box fr-lista"></div>
                </div>
            </div>

            <div class="fr-actions">
                <button id="frLimpar" class="fr-btn secondary" type="button">Limpar</button>
                <button id="frAplicar" class="fr-btn primary" type="button">Aplicar filtro</button>
            </div>
        </div>
    `;

    document.body.appendChild(modal);

    const tiposWrap = modal.querySelector("#frTipos");
    const movWrap = modal.querySelector("#frMovimentos");
    const instWrap = modal.querySelector("#frInstituicoes");
    const catWrap = modal.querySelector("#frCategorias");

    tiposWrap.innerHTML = opcoesTipo.map(item => `
        <label class="fr-check">
            <input type="checkbox" class="fr-tipo" value="${item}">
            ${item === "Transferencia" ? "Transferência" : item}
        </label>
    `).join("");

    movWrap.innerHTML = opcoesMovimento.map(item => `
        <label class="fr-check">
            <input type="checkbox" class="fr-movimento" value="${item}">
            ${item === "Debito" ? "Débito" : item === "Credito" ? "Crédito" : item}
        </label>
    `).join("");

    instWrap.innerHTML = instituicoes.length
        ? instituicoes.map(inst => `
            <label class="fr-check">
                <input type="checkbox" class="fr-inst" value="${inst.id}">
                ${inst.intituicao ? inst.intituicao.nome : "Instituição"}
            </label>
        `).join("")
        : "<small>Nenhuma instituição vinculada.</small>";

    catWrap.innerHTML = categorias.length
        ? categorias.map(cat => `
            <label class="fr-check">
                <input type="checkbox" class="fr-cat" value="${cat.id}">
                ${cat.categoria ? cat.categoria.titulo : "Categoria"}
            </label>
        `).join("")
        : "<small>Nenhuma categoria vinculada.</small>";

    modal.addEventListener("click", e => { if (e.target === modal) modal.remove(); });
    modal.querySelector("#frFechar").addEventListener("click", () => modal.remove());

    const inputData = modal.querySelector("#frDataEvento");
    const btnData = modal.querySelector("#frBtnData");
    const txtData = modal.querySelector("#frDataDisplay");

    btnData.addEventListener("click", () => {
        if (typeof inputData.showPicker === "function") {
            inputData.showPicker();
        } else {
            inputData.click();
        }
    });

    inputData.addEventListener("change", () => {
        if (!inputData.value) {
            txtData.textContent = "Nenhuma data selecionada";
            return;
        }
        const [ano, mes, dia] = inputData.value.split("-");
        txtData.textContent = `${dia}/${mes}/${ano}`;
    });

    modal.querySelector("#frLimpar").addEventListener("click", () => {
        modal.querySelector("#frCampoTexto").value = "titulo";
        modal.querySelector("#frTextoBusca").value = "";
        modal.querySelector("#frDataEvento").value = "";
        modal.querySelector("#frDataDisplay").textContent = "Nenhuma data selecionada";
        modal.querySelectorAll("input[type='checkbox']").forEach(c => { c.checked = false; });
    });

    modal.querySelector("#frAplicar").addEventListener("click", async () => {
        const campoTexto = modal.querySelector("#frCampoTexto").value;
        const textoBusca = modal.querySelector("#frTextoBusca").value.trim();
        const dataEvento = modal.querySelector("#frDataEvento").value;
        const tipo = Array.from(modal.querySelectorAll(".fr-tipo:checked")).map(i => i.value);
        const tipoMovimento = Array.from(modal.querySelectorAll(".fr-movimento:checked")).map(i => i.value);
        const instituicaoUsuario = Array.from(modal.querySelectorAll(".fr-inst:checked")).map(i => i.value);
        const categoriaUsuario = Array.from(modal.querySelectorAll(".fr-cat:checked")).map(i => i.value);

        const filtroTexto = {
            titulo: campoTexto === "titulo" ? textoBusca : "",
            descricao: campoTexto === "descricao" ? textoBusca : ""
        };

        try {
            const filtrados = await MainAPI.filtrarRegistros(userId, {
                ...filtroTexto,
                dataEvento,
                tipo,
                tipoMovimento,
                instituicaoUsuario,
                categoriaUsuario
            });

            renderizarRegistros(filtrados);
            modal.remove();
        } catch (e) {
            console.error("Erro ao filtrar registros:", e);
        }
    });
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