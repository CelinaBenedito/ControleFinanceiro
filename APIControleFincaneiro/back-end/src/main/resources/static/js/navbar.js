const navbar = document.getElementById("navbar");
const main = document.getElementById("main");
const home = document.getElementById("home");
const reg = document.getElementById("reg");
const add = document.getElementById("add");
const agenda = document.getElementById("agenda");
const loja = document.getElementById("loja");
const config = document.getElementById("config");
const tema = document.getElementById("tema");

let ativo = false;

navbar.style.width = "70px";
home.style.display = "none";
reg.style.display = "none";
add.style.display = "none";
agenda.style.display = "none";
if (loja) loja.style.display = "none";
config.style.display = "none";
tema.style.display = "none";
main.style.marginLeft = "70px";

function sidebarFunction() {
    if (!ativo) {
        ativo = true;
        navbar.style.width = "290px";
        main.style.marginLeft = "290px";
        home.style.display = "";
        reg.style.display = "";
        add.style.display = "";
        agenda.style.display = "";
        if (loja) loja.style.display = "";
        config.style.display = "";
        tema.style.display = "";
    } else {
        ativo = false;
        navbar.style.width = "70px";
        main.style.marginLeft = "70px";
        home.style.display = "none";
        reg.style.display = "none";
        add.style.display = "none";
        agenda.style.display = "none";
        if (loja) loja.style.display = "none";
        config.style.display = "none";
        tema.style.display = "none";
    }
}

/*---------------- Modal de confirmação de Logout ----------------*/
(function () {
    // Cria o modal de confirmação de logout dinamicamente
    const modalHtml = `
    <div id="uwLogoutOverlay" style="
        display:none; position:fixed; inset:0; z-index:99999;
        background:rgba(0,0,0,0.45); backdrop-filter:blur(2px);
        align-items:center; justify-content:center;">
        <div id="uwLogoutModal" style="
            background:var(--cor-fundo-card, #fff);
            border-radius:18px;
            box-shadow:0 16px 48px rgba(0,0,0,0.28);
            padding:36px 32px 28px;
            max-width:400px; width:90%;
            display:flex; flex-direction:column; align-items:center; gap:16px;
            animation:uwModalIn 0.2s ease;">
            <div style="
                width:60px; height:60px; border-radius:50%;
                background:var(--red-100,#fee2e2);
                display:flex; align-items:center; justify-content:center;">
                <i class='bx bx-log-out' style="font-size:1.8rem; color:var(--red-700,#b91c1c);"></i>
            </div>
            <h2 style="
                font-size:1.15rem; font-weight:700; margin:0;
                color:var(--cor-titulo); text-align:center;">
                Fazer Logout?
            </h2>
            <p style="
                font-size:0.9rem; color:var(--cor-texto-secundario);
                text-align:center; margin:0; line-height:1.55;">
                Isso irá retirar sua conta deste dispositivo.<br>
                Para acessar novamente, você deverá fazer login.
            </p>
            <div style="display:flex; gap:12px; width:100%; margin-top:8px;">
                <button id="uwLogoutCancelar" style="
                    flex:1; padding:11px; border-radius:10px; cursor:pointer;
                    background:var(--cor-fundo-pagina); color:var(--cor-texto-principal);
                    font-size:0.9rem; font-weight:600; border:1px solid var(--cor-tinte-borda, #ccc);
                    transition:background 0.18s; margin:0;">
                    Cancelar
                </button>
                <button id="uwLogoutConfirmar" style="
                    flex:1; padding:11px; border-radius:10px; cursor:pointer;
                    background:var(--red-700,#b91c1c); color:#fff;
                    font-size:0.9rem; font-weight:600; border:none;
                    transition:background 0.18s; margin:0;">
                    Sim, fazer logout
                </button>
            </div>
        </div>
    </div>
    <style>
        @keyframes uwModalIn {
            from { opacity:0; transform:scale(0.92) translateY(12px); }
            to   { opacity:1; transform:scale(1)    translateY(0);     }
        }
        #uwLogoutCancelar:hover  { background:var(--cor-hover, #e2e8f0) !important; }
        #uwLogoutConfirmar:hover { background:#991b1b !important; }
    </style>`;

    document.body.insertAdjacentHTML('beforeend', modalHtml);

    const overlay  = document.getElementById('uwLogoutOverlay');
    const btnCanc  = document.getElementById('uwLogoutCancelar');
    const btnConf  = document.getElementById('uwLogoutConfirmar');

    window._abrirLogoutModal = function () {
        overlay.style.display = 'flex';
    };

    btnCanc.addEventListener('click', function () {
        overlay.style.display = 'none';
    });

    overlay.addEventListener('click', function (e) {
        if (e.target === overlay) overlay.style.display = 'none';
    });

    btnConf.addEventListener('click', function () {
        const user = JSON.parse(localStorage.getItem('usuarioLogado') || 'null');
        if (user && user.id) {
            let perfis = JSON.parse(localStorage.getItem('perfis') || '[]');
            perfis = perfis.filter(function (p) { return p.id !== user.id; });
            localStorage.setItem('perfis', JSON.stringify(perfis));
        }
        localStorage.removeItem('usuarioLogado');
        window.location.href = 'index.html';
    });
})();

/*---------------- User widget dropdown ----------------*/
(function () {
    const userWidget = document.getElementById('userWidget');
    const dropdown   = document.getElementById('uwDropdown');
    const btnLogout  = document.getElementById('btnLogout');

    if (!userWidget || !dropdown) return;

    // Abre/fecha o dropdown ao clicar no widget
    userWidget.addEventListener('click', function (e) {
        e.stopPropagation();
        dropdown.classList.toggle('aberto');
    });

    // Fecha ao clicar fora
    document.addEventListener('click', function () {
        dropdown.classList.remove('aberto');
    });

    // Impede que cliques dentro do dropdown fechem ele imediatamente
    dropdown.addEventListener('click', function (e) {
        e.stopPropagation();
    });

    // LogOut: abre modal de confirmação
    if (btnLogout) {
        btnLogout.addEventListener('click', function () {
            dropdown.classList.remove('aberto');
            window._abrirLogoutModal();
        });
    }
})();

/*---------------- User widget ----------------*/
(function () {
    const uwNome = document.getElementById("uw_nome");
    const uwXp = document.getElementById("uw_xp");
    const uwLvl = document.getElementById("uw_lvl");
    const uwAvatar = document.querySelector(".uw-avatar");
    if (!uwNome) return;

    const user = JSON.parse(localStorage.getItem("usuarioLogado") || "null");
    if (user && user.nome) uwNome.textContent = user.nome;

    function renderizarAvatarWidget(usuarioAtual) {
        if (!uwAvatar) return;
        const url = window.MainAPI?.resolverUrlImagem
            ? window.MainAPI.resolverUrlImagem(usuarioAtual?.imagem, usuarioAtual?.id)
            : null;

        if (url) {
            const urlFinal = /^data:image\//i.test(url)
                ? url
                : `${url}${url.includes("?") ? "&" : "?"}v=${Date.now()}`;
            uwAvatar.innerHTML = `<img src="${urlFinal}" alt="Foto de perfil" style="width:100%;height:100%;object-fit:cover;border-radius:50%;" />`;
        } else {
            uwAvatar.innerHTML = "<i class='bx bx-user' style='font-size:1.6rem; color:var(--cor-principal);'></i>";
        }
    }

    renderizarAvatarWidget(user);

    function xpNecessarioDoNivel(nivelAtual) {
        return Math.round(500 * Math.pow(nivelAtual, 1.5));
    }

    function calcularNivelEProgresso(xpTotal) {
        const xpSeguro = Number.isFinite(xpTotal) && xpTotal > 0 ? xpTotal : 0;
        let nivel = 1;
        let xpNoNivel = xpSeguro;
        let xpProximoNivel = xpNecessarioDoNivel(nivel);

        while (xpNoNivel >= xpProximoNivel) {
            xpNoNivel -= xpProximoNivel;
            nivel += 1;
            xpProximoNivel = xpNecessarioDoNivel(nivel);
        }

        const progresso = xpProximoNivel > 0
            ? Math.max(0, Math.min(100, (xpNoNivel / xpProximoNivel) * 100))
            : 0;

        return { nivel, xpNoNivel, xpProximoNivel, progresso };
    }

    async function atualizarXPWidget() {
        if (!uwXp || !uwLvl || !user?.id) return;
        try {
            const res = await fetch(`http://localhost:8080/usuarios/calculo-xp/${user.id}`);
            if (!res.ok) {
                uwXp.style.width = "0%";
                uwLvl.textContent = "LVL 1";
                return;
            }

            const xp = Number(await res.json());
            const info = calcularNivelEProgresso(xp);

            uwXp.style.width = `${info.progresso.toFixed(2)}%`;
            uwLvl.textContent = `LVL ${info.nivel}`;
            uwLvl.title = `${Math.floor(info.xpNoNivel)}/${info.xpProximoNivel} XP`;
        } catch (e) {
            console.error("Erro ao atualizar XP:", e);
        }
    }

    window.atualizarXPWidget = atualizarXPWidget;
    window.addEventListener("xp:refresh", atualizarXPWidget);
    window.addEventListener("usuario:imagemAtualizada", () => {
        const usuarioAtual = JSON.parse(localStorage.getItem("usuarioLogado") || "null");
        renderizarAvatarWidget(usuarioAtual);
    });
    atualizarXPWidget();
})();

/*---------------- Tema dark ----------------*/
const modoSalvo = localStorage.getItem("modo");

if (modoSalvo === "dark") {
    document.body.setAttribute("data-mode", "dark");
}

const temas = document.querySelectorAll(".pf-tema-item");
const btnEscolher = document.getElementById("btnEscolherTema");

let temaSelecionado = "padrao";

temas.forEach((tema) => {
    tema.addEventListener("click", () => {
        temas.forEach(t => t.classList.remove("ativo"));
        tema.classList.add("ativo");
        temaSelecionado = tema.dataset.tema;
    });
});

document.getElementById("toggleTheme").addEventListener("click", () => {

    const modo =  document.body.getAttribute("data-mode") == "dark" ? "light" : "dark";
    document.body.setAttribute("data-mode", modo);
    localStorage.setItem("modo", modo);
    atualizarIconeTema();
    console.log("mudei")
});

window.addEventListener("DOMContentLoaded", () => {

    const temaSalvo = localStorage.getItem("tema");
    const modoSalvo = localStorage.getItem("modo");

    if (temaSalvo) {
        document.body.setAttribute("data-tema", temaSalvo);
        temas.forEach(t => {
            t.classList.remove("ativo");

            if (t.dataset.tema === temaSalvo) {
                t.classList.add("ativo");
            }
        });
        temaSelecionado = temaSalvo;
    }

    if (modoSalvo) {
        document.body.setAttribute("data-mode", modoSalvo);
        atualizarIconeTema();
        console.log("mudei 2")
    }
});

function atualizarIconeTema() {
    const icone = document.getElementById("icone");
    const modoSalvo = localStorage.getItem("modo");
    if (modoSalvo === "dark") {
        icone.innerHTML = "<i class='bx bx-sun'></i>";
            console.log("cheguei 2")

    } else {
        icone.innerHTML = "<i class='bx bx-moon'></i>";
            console.log("cheguei 1")

    }
}

atualizarIconeTema();