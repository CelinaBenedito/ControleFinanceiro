
div_alerta.style.display = 'none';

function alerta(texto) {
    div_alerta.style.display = "flex"
    conteudoAlerta.innerHTML =
        `
        ${texto}
        `
}

function checarDados(){
    const email = document.getElementById("ipt_email").value.trim();
    const senha = document.getElementById("ipt_senha").value;

    console.warn("%cEntrou na função checarDados", "color: orange; font-weight: bold");

/* =========================
       EMAIL
    ========================= */

    const regexEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!regexEmail.test(email)) {
        alerta(`Email inválido <button onclick='div_alerta.style.display="none"'>OK</button>`);
        console.error("Email inválido");
        return;
    }
    console.log("%cEmail OK", "color: green");

    /* =========================
       SENHA
       - 6 a 25 caracteres
       - 1 letra maiúscula
       - 1 caractere especial
    ========================= */

    const regexSenha = /^(?=.*[A-Z])(?=.*[@$!%*?&.#])[A-Za-z\d@$!%*?&.#]{6,25}$/;

    if (!regexSenha.test(senha)) {
        alerta(`
            Senha inválida<br>
            • 6 a 25 caracteres<br>
            • 1 letra maiúscula<br>
            • 1 caractere especial
            <button onclick='div_alerta.style.display="none"'>OK</button>
        `);
        console.error("Senha inválida");
        return;
    }

    console.log("%cSenha OK", "color: green");

     console.log(
        "%c✔ Todos os dados validados com sucesso!",
        "color: #16a34a; font-weight: bold; font-size: 14px"
    );
    console.warn("Redirecionando para o login!")

    login(email, senha);
}

function login(email, senha){
    console.warn("Iniciando o login!");

    fetch(`http://localhost:8080/usuarios/login`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            email: email,
            senha: senha
        }),
    }).then((response) => {
        console.warn("Resposta da tentativa de login:", response);
        if(response.ok){
            alerta(`Logado com sucesso!`);

             setTimeout(() => {
        window.location.href = "dashboard.html";
    }, 1500);
        }
    })

}