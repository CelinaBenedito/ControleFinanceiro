const navbar = document.getElementById("navbar");
const main = document.getElementById("main");
const home = document.getElementById("home");
const reg = document.getElementById("reg");
const add = document.getElementById("add");
const agenda = document.getElementById("agenda");
const config = document.getElementById("config");
const tema = document.getElementById("tema");

let ativo = false;

navbar.style.width = "70px";
home.style.display = "none";
reg.style.display = "none";
add.style.display = "none";
agenda.style.display = "none";
config.style.display = "none";
tema.style.display = "none";
main.style.marginLeft = "70px";


function sidebarFunction() {
    console.log("Entrei na funciton", ativo)
    if (!ativo) {
        ativo = true;

        navbar.style.width = "290px";
        main.style.marginLeft = "290px";
        home.style.display = "";
        reg.style.display = "";
        add.style.display = "";
        agenda.style.display = "";
        config.style.display = "";
        tema.style.display = "";

        console.log("Abriu", ativo);

    } else {
        ativo = false;

        navbar.style.width = "70px";
        main.style.marginLeft = "70px";
        home.style.display = "none";
        reg.style.display = "none";
        add.style.display = "none";
        agenda.style.display = "none";
        config.style.display = "none";
        tema.style.display = "none";

        console.log("Fechou", ativo);
    }

}

/*---------------- Tema dark ----------------*/
const theme = localStorage.getItem("theme");

if (theme === "dark") {
    document.body.classList.add("dark-mode");
}

/*---------------- User widget ----------------*/
(function () {
    const uwNome = document.getElementById("uw_nome");
    const uwXp = document.getElementById("uw_xp");
    const uwLvl = document.getElementById("uw_lvl");
    if (!uwNome) return;

    const user = JSON.parse(localStorage.getItem("usuarioLogado") || "null");
    if (user && user.nome) uwNome.textContent = user.nome;

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
    atualizarXPWidget();
})();

document.getElementById("toggleTheme").addEventListener("click", () => {
    document.body.classList.toggle("dark-mode");

    localStorage.setItem(
        "theme",
        document.body.classList.contains("dark-mode") ? "dark" : "light"
    );
    atualizarIconeTema();
});

function atualizarIconeTema() {
    const icone = document.getElementById("icone");
    if (document.body.classList.contains("dark-mode")) {
        icone.innerHTML = "<i class='bx bx-sun'></i>";
    } else {
        icone.innerHTML = "<i class='bx bx-moon'></i>";
    }
}

atualizarIconeTema();