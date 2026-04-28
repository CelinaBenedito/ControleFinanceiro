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

                                    cardsDiv.innerHTML += `
                                        <div class="cardRegistro">
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
                                        </div>
                                    `;
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