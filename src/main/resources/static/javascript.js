


window.onload = async function() {
    await document.querySelectorAll(".conteudo-container").forEach(td => {
        const targetDiv = td.firstElementChild;
        teste(targetDiv)
    })

    await document.querySelectorAll("#contatos").forEach(td => {
        const targetDiv = td.firstElementChild;
        trocarFormContato(targetDiv)
    })

};


function trocarFormContato(button){
    var div = button.parentNode;
    var F = div.querySelector("#F");
    var T = div.querySelector("#T");
    if (T.style.display === ''){
        button.innerHTML = 'Add contato'
        T.style.display = 'none';
        F.style.display = '';
        return;
    }
    if(T.style.display === 'none'){
        button.innerHTML = 'Lista de contatos'
        F.style.display = 'none';
        T.style.display = '';
    }
}


async function teste(teste) {
    var cidadeIntra = teste.id;
    var button = teste;
    var url = "https://intra.mp.rs.gov.br/site/promotorias/" + cidadeIntra + "/";

    let targetDiv = button.parentElement.querySelector("#conteudo");
    if(targetDiv.innerHTML.length > 3){
        targetDiv.innerHTML = ""
        return
    }

    try {
        const response = await fetch(url);
        const text = await response.text();
        const tempDiv = document.createElement("div");
        tempDiv.innerHTML = text;

        const conteudoDesejado = tempDiv.querySelector(".details");

        if (conteudoDesejado) {
            let targetDiv = button.parentElement.querySelector("#conteudo");
            targetDiv.innerHTML = conteudoDesejado.innerHTML;
        } else {
            let targetDiv = button.parentElement.querySelector("#conteudo");
            targetDiv.innerHTML = "Sem contato na intra"
            console.log("Div não encontrada!");
        }
    } catch (error) {
        console.error("Erro ao carregar conteúdo:", error);
    }
}




