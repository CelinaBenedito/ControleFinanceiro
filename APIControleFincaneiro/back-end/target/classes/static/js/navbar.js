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