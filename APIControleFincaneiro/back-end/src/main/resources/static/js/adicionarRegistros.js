div_alerta.style.display = 'none';
gestaoConta.style.display = "none";

let dataGasto;
const API_BASE = window.location.port === "8080" ? "" : "http://localhost:8080";

function apiUrl(path) {
    return `${API_BASE}${path}`;
}

function alerta(texto) {
    div_alerta.style.display = "flex"
    conteudoAlerta.innerHTML =
        `
        ${texto}
        `
}

function gerarInformacoes() {
    gerarTipos();
    gerarInstituicao();
}

function gerarTipos() {
    const usuario = JSON.parse(localStorage.getItem('usuario') || '{}');
    select_tipo.innerHTML = "<option value='#'>Escolha um tipo</option>"
    fetch(apiUrl(`/categorias/usuario/${usuario.id}`), {
        method: "GET"
    }).then(res => {
        if (res.status === 204) return;
        res.json().then(json => {
            for (let c = 0; json.length > c; c++) {
                select_tipo.innerHTML +=
                    `<option value="${json[c].categoria.id}">${json[c].categoria.titulo}</option>`
            }
        })
    })
}

async function gerarInstituicao() {
    const usuario = JSON.parse(localStorage.getItem('usuario') || '{}');
    gestaoInstituicao = document.getElementById("gestaoInstituicao")
    select_instituicao.innerHTML = "<option value='#'> Escolha uma instituição</option>"
    fetch(apiUrl(`/instituicoes/usuarios/${usuario.id}`), {
        method: "GET"
    }).then(res => {
        if (res.status === 204) return;
        res.json().then(json => {
            for (let c = 0; json.length > c; c++) {
                select_instituicao.innerHTML +=
                    `<option value="${json[c].id}">${json[c].nome}</option>`

                gestaoInstituicao.innerHTML +=
                    `<option onclick="controleInstituicao()">${json[c].nome}</option>`
            }
        })
    })
}

async function controleInstituicao(){
    console.log("entrei no controle de instituição")
    escondido = document.getElementById("escondido")

    escondido.style.display = ""

}

function registrar() {
    ("Iniciando registro!")
    var data = dataGasto;
    var valor = Number(document.getElementById('ipt_valor').value);
    var titulo = ipt_nome.value;
    var tipo = select_tipo.value;
    var Desc = ipt_desc.value;
    var instituicao = select_instituicao.value

    if (Desc == "" || Desc == false) {
        Desc = "Nenhuma descrição fornecida"
    }
    if (data == false || data == 0) {
        return alert("Data inválida");
    }
    if (valor <= 0 || valor == null || valor == undefined || valor == NaN) {
        return alert("Valor inválido");
    }
    if (titulo == "") {
        return alert("Titulo inválido");
    }
    if (tipo == '#') {
        return alert("Escolha um tipo");
    }
    if (instituicao == '#') {
        return alert("Escolha uma instituição");
    }

    setTimeout(alerta(`Registrando...
        <div class="glaceonCorrendoDiv">
    <img class="glaceon correndo" src="/assets/gif/glaceon-correndo-unscreen.gif" alt="">
    </div>
        `), 2000)

    fetch("/registros/registrar", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            valorServer: valor,
            descServer: Desc,
            tipoServer: tipo,
            tituloServer: titulo,
            dataServer: data,
            instituicaoServer: instituicao
        }),
    }).then((response) => {
        console.log("Resposta:", response);
        if (response.ok) {
            atualizarSaldo(valor, instituicao);
            return setTimeout(() => alerta(
                `  
                        Registro realizado com sucesso!<br>
                        <div>

                        <button onclick='window.location.reload()'>
                            Continuar a registrar
                        </button>

                        </div>
                `
            ), 3000);

        }
        else {
            alert("Houve um erro ao registrar", response)
        }
    });

}

function atualizarSaldo(valor, instituicao) {
    console.log("chamou atualizar saldo")
    fetch(`/registros/atualizarSaldo`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            valorServer: valor,
            instituicaoServer: instituicao
        }),
    }).then((resposta) => {
        console.log("Resposta:", resposta);
        if (resposta.ok) {
            return console.log("Saldo atualizado")
        }
        else {
            console.error("Houver um erro ao atualizar o saldo", resposta)
        }
    });
}

function adicionarTipos() {
    const usuario = JSON.parse(localStorage.getItem('usuario') || '{}');
    if (!usuario.id) {
        alerta(`Faça login novamente para adicionar tipo <button onclick="div_alerta.style.display='none'">OK</button>`);
        return;
    }
    const titulo = ipt_tituloTipo.value.trim();
    if (!titulo) {
        return alerta(`Informe o nome do tipo <button onclick="div_alerta.style.display='none'">OK</button>`);
    }
    fetch(apiUrl('/categorias'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ titulo: titulo })
    }).then(res => {
        if (res.ok) return res.json();
        if (res.status === 409) {
            return fetch(apiUrl('/categorias'))
                .then(r => r.json())
                .then(all => {
                    const found = all.find(c => c.titulo.toLowerCase() === titulo.toLowerCase());
                    if (!found) throw new Error('Categoria não encontrada');
                    return found;
                });
        }
        throw new Error('Erro ao criar tipo');
    }).then(cat => {
        return fetch(apiUrl(`/categorias/${cat.id}/usuarios/${usuario.id}`), { method: 'POST' });
    }).then(resposta => {
        if (resposta.ok || resposta.status === 201) {
            gerarTipos();
            adicionarTipo.close();
            ipt_tituloTipo.value = '';
            alerta(`Tipo adicionado com sucesso!<br><button onclick="div_alerta.style.display='none'">OK</button>`);
        } else if (resposta.status === 409) {
            alerta(`Você já possui esse tipo <button onclick="div_alerta.style.display='none'">OK</button>`);
        } else {
            alerta(`Houve um erro ao adicionar tipo <button onclick="div_alerta.style.display='none'">OK</button>`);
        }
    }).catch(err => {
        alerta(`${err.message} <button onclick="div_alerta.style.display='none'">OK</button>`);
    });
}

/*-------------- Calendário --------------*/

const btnAbrir = document.getElementById("calendario");
const modal = document.getElementById("modal");
const fechar = document.getElementById("fechar");
const dias = document.getElementById("dias");
const mesAno = document.getElementById("mesAno");
const gastosDia = document.getElementById("gastosDia");
const confirmar = document.getElementById("confirmar");
const btnAnterior = document.getElementById("btnAnterior");
const btnProximo = document.getElementById("btnProximo");

let dataSelecionada = null;

let hoje = new Date();
let mesAtual = hoje.getMonth();
let anoAtual = hoje.getFullYear();


btnAbrir.onclick = () => modal.style.display = "flex";
fechar.onclick = () => modal.style.display = "none";
btnAnterior.onclick = () => {
    mesAtual--;
    if (mesAtual < 0) {
        mesAtual = 11;
        anoAtual--;
    }
    gerarCalendario();
}
btnProximo.onclick = () => {
    mesAtual++;
    if (mesAtual > 11) {
        mesAtual = 0;
        anoAtual++;
    }
    gerarCalendario();
}

const gastos = {
    "2025-01-05": ["R$ 25,00 - Café", "R$ 80,00 - Mercado"],
    "2025-01-10": ["R$ 14,00 - Transporte"],
};

async function buscarGastosDia(dataSelecionada) {
    console.log("Buscando gastos para", dataSelecionada);

    const res = await fetch(apiUrl(`/registros/buscarData/${dataSelecionada}`), {
        method: "GET"
    });

    if (!res.ok) {
        console.warn("Falha ao buscar gastos do dia:", res.status);
        return [];
    }

    const json = await res.json();
    console.log("Tamanho de gastos:", json.length);

    let listaGastos = [];

    for (let c = 0; c < json.length; c++) {
        const data = new Date(json[c].dataGasto);
        const dataFormatada = data.toISOString().split("T")[0];

        if (dataFormatada === dataSelecionada) {
            listaGastos.push(json[c]);
        }
    }

    return listaGastos;
}


function gerarCalendario() {
    dias.innerHTML = "";
    let primeiroDia = new Date(anoAtual, mesAtual, 1).getDay();
    let totalDias = new Date(anoAtual, mesAtual + 1, 0).getDate();

    mesAno.innerText = new Date(anoAtual, mesAtual)
        .toLocaleString("pt-BR", { month: "long", year: "numeric" });

    for (let i = 0; i < primeiroDia; i++) {
        dias.innerHTML += `<span></span>`;
    }
    for (let dia = 1; dia <= totalDias; dia++) {
        let dataFormatada = `${anoAtual}-${String(mesAtual + 1).padStart(2, '0')}-${String(dia).padStart(2, '0')}`;

        let span = document.createElement("span");
        span.innerText = dia;

        span.onclick = () => selecionarDia(dataFormatada, span);

        dias.appendChild(span);
    }
}

async function selecionarDia(data, elemento) {
    dataP = document.getElementById('data')
    dataSelecionada = data;

    document.querySelectorAll(".diaSelecionado")
        .forEach(e => e.classList.remove("diaSelecionado"));

    elemento.classList.add("diaSelecionado");

    try {
        const listaGastos = await buscarGastosDia(data);

        console.log("tamanho lista gastos: ",listaGastos.length)
        if (listaGastos.length > 0) {
            console.log("Lista de gastos > 0")
            novaData = new Date(listaGastos[0].dataGasto);
            const dataFormatada = novaData.toLocaleDateString("pt-BR");
            gastosDia.innerHTML = `
            <b>Gastos de ${dataFormatada}:</b><br>
        `;
            for (let c = 0; c < listaGastos.length; c++) {
                gastosDia.innerHTML += `
            <b>${listaGastos[c].tituloGasto} - R$${listaGastos[c].valor}</b><br>
        `;
            }
        } else {
            gastosDia.innerHTML = "<i>Nenhum gasto neste dia.</i>";
        }
    } catch (erro) {
        console.error("Erro ao carregar gastos do dia:", erro);
        gastosDia.innerHTML = "<i>Não foi possível carregar os gastos, mas você pode confirmar a data.</i>";
    }

    confirmar.disabled = false;
}

confirmar.onclick = () => {
    modal.style.display = "none";
    dataP.style.display = ""
    dataFormatada = formatarDataBR(dataSelecionada)
    dataP.textContent = dataFormatada
    dataP.classList.remove("hidden");
    dataGasto = dataSelecionada;
};

gerarCalendario();

function formatarDataBR(dataISO) {
    const [ano, mes, dia] = dataISO.split("-");
    return `${dia}/${mes}/${ano}`;
}

/*---------------- Parte do Header ----------------*/
const titulos = document.querySelectorAll(".headerFormulario .titulo");

titulos.forEach(item => {
    item.addEventListener("click", () => {

        // Remove classe 'ativo' de todos
        titulos.forEach(e => e.classList.remove("ativo"));

        // Adiciona no clicado
        item.classList.add("ativo");

        // Identificar qual aba foi selecionada (opcional)
        let aba = item.dataset.form;
        console.log("Aba selecionada:", aba);

        // aqui você pode trocar telas, formularios etc
    });
});

function trocarFormulario(tela) {
    if (tela == "gestaoConta") {
        gestaoConta.style.display = "";
        addGasto.style.display = "none";
    } else {
        gestaoConta.style.display = "none";
        addGasto.style.display = "";
    }

}


