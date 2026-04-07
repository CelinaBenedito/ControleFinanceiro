function carregarRegistros() {
    fetch("/registros/carregarRegistros")
        .then(res => res.json())
        .then(json => {

            registros.innerHTML = "";

            const formatadorMoeda = new Intl.NumberFormat("pt-BR", {
                style: "currency",
                currency: "BRL"
            });

            const agrupado = {};

            json.forEach(registro => {
                const data = new Date(registro.dataGasto);
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
                                .sort((a, b) => new Date(a.dataGasto) - new Date(b.dataGasto))
                                .forEach(registro => {

                                    const data = new Date(registro.dataGasto);
                                    const dia = String(data.getDate()).padStart(2, "0");

                                    cardsDiv.innerHTML += `
                                        <div class="cardRegistro">
                                            <div class="dataRegistro">${dia}</div>

                                            <div class="registroInfo">
                                                <div class="registroTitulo">${registro.tituloGasto}</div>
                                                <div class="registroDescricao">${registro.descricao}</div>
                                            </div>

                                            <div class="registroDetalhes">
                                                <div class="registroValor">
                                                    ${formatadorMoeda.format(registro.valor)}
                                                </div>
                                                <div class="registroTipo">${registro.tituloTipo}</div>
                                                <div class="registroInstituicao">${registro.nomeInstituicao}</div>
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
    fetch("/registros/adicionarSaldo", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            valorServer: valor,
            instituicaoServer: instituicao
        })
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
    fetch("/registros/atualizarSaldo", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            valorServer: valor,
            instituicaoServer: instituicao
        })
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
    fetch("/registros/gerarInstituicoes", {
        method: "GET"
    }).then(res => {
        res.json().then(json => {
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
    })
}
fetch("/registros/mostrarSaldoTotal", {
    method: "GET"
}).then(res => {
    res.json().then(json => {
        ValorTotal.innerHTML = json[0].valorTotal
    })
})

function Consulta() {
    principal.innerHTML = "    "
    fetch("/registros/mostrarTodasInstituicoes", {
        method: "GET"
    }).then(res => {
        res.json().then(json => {
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
    })


}