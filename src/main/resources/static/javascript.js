
document.addEventListener("DOMContentLoaded", () => {
    console.log("DOM pronto");
});

window.addEventListener("load", () => {
    const loader = document.getElementById("loader");

    // fade out
    loader.classList.add("hidden");

    // remove do DOM depois da animação
    setTimeout(() => {
        loader.remove();
    }, 500);
});

document.onreadystatechange = () => {
    console.log("Estado:", document.readyState);
};

window.onload = async function() {
    await document.querySelectorAll(".conteudo-container").forEach(td => {
        const targetDiv = td.firstElementChild;
        contatosIntra(targetDiv)
        teste(targetDiv)
        carregarResultados()

    })

    await document.querySelectorAll("#contatos").forEach(td => {
        const targetDiv = td.firstElementChild;
        trocarFormContato(targetDiv)
    })
    carregarResultados()
};

function atribuir(elem) {
    const at = document.getElementById("atribuidor" + elem.dataset.id);
    if (!at) return;

    const isHidden = window.getComputedStyle(at).display === "none";
    at.style.display = isHidden ? "grid" : "none";
}

document.addEventListener("click", function (event) {

    const at = document.getElementById("atribuidor");
    if (!at) return;

    const isHidden = window.getComputedStyle(at).display === "none";

    // Se estiver fechado, não faz nada
    if (isHidden) return;

    // Se clicou dentro da div, não fecha
    if (at.contains(event.target)) return;

    // Se clicou no botão que chama atribuir(), não fecha
    if (event.target.closest("[onclick='atribuir()']")) return;

    // Caso contrário, fecha
    at.style.display = "none";
});


function getGraficoSmoke(smokeid) {
    const container = document.getElementById(smokeid.dataset.cidade + smokeid.dataset.id);
    // Adicione a barra inicial para garantir que comece da raiz do contexto
    container.src = "https://upload.wikimedia.org/wikipedia/commons/b/b1/Loading_icon.gif"
    fetch(`/pegarGraficoSmoke/` + smokeid.dataset.cidade)
        .then(response => {
            if (!response.ok) throw new Error("Erro na requisição: " + response.status)
            return response.text();
        })
        .then(url => {
            container.src = url;
        })
        .catch(error => console.error('Erro ao buscar gráfico:', error));
}


async function contatosIntra(info) {
    var cidadeIntra = info.id;
    var button = info;
    var url = "https://intra.mp.rs.gov.br/site/promotorias/" + cidadeIntra + "/";

    let targetDiv = button.parentElement.querySelector("#ctc");
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
            let targetDiv = button.parentElement.querySelector("#ctc");
            targetDiv.innerHTML = conteudoDesejado.innerHTML;
        } else {
            let targetDiv = button.parentElement.querySelector("#ctc");
            targetDiv.innerHTML = "Sem contato na intra"
            console.log("Div não encontrada!");
        }
    } catch (error) {
        console.error("Erro ao carregar conteúdo:", error);
    }
}

function carregarResultados() {
    let resultados = document.querySelectorAll(".resultado");
    resultados.forEach(div => {
        div.addEventListener("mouseenter",hoverHandler)
        div.addEventListener("mouseleave",hoverHandler)
        let res = div.getAttribute("data-resultado");
        let vel = div.getAttribute("data-velocidade");
        let dia = div.getAttribute("data-dia");

        // separa por "|"
        let partes = res.split("|");
        let rx;
        let tx;
        try {
             rx = parseFloat(partes[0].replace("RX:", "").trim());
             tx = parseFloat(partes[1].replace("TX:", "").trim());
        }catch (error){
            console.log(error)
        }

        if(vel.includes("20")){
            if(rx > 17 && tx > 17){
                div.className = "resultadoOK"
            }
            else{
                div.className = "resultadoNAOOK"
            }

        }
        else if (vel.includes("50")){
            if(rx > 43 && tx > 43){
                div.className = "resultadoOK"
            }
            else{
                div.className = "resultadoNAOOK"
            }
        }
        else if (vel.includes("100")){
            if(rx > 92 && tx > 92){
                div.className = "resultadoOK"
            }
            else{
                div.className = "resultadoNAOOK"
            }
        }
        else if (vel.includes("200")){
            if(rx > 190 && tx > 190){
                div.className = "resultadoOK"
            }
            else{
                div.className = "resultadoNAOOK"
            }
        }
        div.innerHTML = dia + " 🠖";
    });
}


function hoverHandler(event) {
    const el = event.target;
    if (event.type === "mouseenter") {
        if (!el.querySelector(".popup-resultado")) {
            const popup = document.createElement("div");
            popup.classList.add("popup-resultado");
            popup.textContent = el.getAttribute("data-resultado");
            popup.style.position = "absolute";
            popup.style.top = "100%";        // aparece abaixo do elemento
            popup.style.left = "0";
            popup.style.color = "rgba(0, 0, 0, 1)"
            popup.style.backgroundColor = "yellow";
            popup.style.padding = "5px 10px";
            popup.style.border = "1px solid #ccc";
            popup.style.borderRadius = "4px";
            popup.style.whiteSpace = "nowrap";
            popup.style.zIndex = "1000";
            popup.style.fontSize = "1.5em";
            el.style.position = "relative";
            el.appendChild(popup);
        }

    } else if (event.type === "mouseleave") {
        const popup = el.querySelector(".popup-resultado");
        if (popup) {
            popup.remove();
        }
    }
}

var ligarHistorico = false;
function mostrarHistorico(){
    let resultados = document.querySelectorAll(".background");
    let trs = document.querySelectorAll("tr");

    resultados.forEach(res =>{
        res.style.display = ligarHistorico ? "none" : "flex";

    }
    )
    trs.forEach(trs =>{
        trs.className = ligarHistorico ? "linha" : "linha";
    })

    ligarHistorico = !ligarHistorico;
}


// remove acentos
function limpaAcentos(str){
    return str.normalize("NFD").replace(/[\u0300-\u036f]/g, "");
}

function filterTable() {
    //pega o elemento search, que é a barra de pesquisa, passamos tudo para lower case para evitar erros.
    let input = document.getElementById("search");
    let filter = limpaAcentos(input.value.toLowerCase())

    // pegamos a tabela as linhas
    let table = document.getElementById("dataTable").querySelector("tbody");
    let tr = table.getElementsByTagName("tr");

    // iteramos as linhas e adicionamos uma variavel de match para caso encontre o que o usuário esta digitando.
    for (let i = 0; i < tr.length; i++) {
        let td = tr[i].getElementsByTagName("td");
        let match = false;
        //iteramos as colunas a procura do texto do filter que o usuário digitou. caso encontre retorna true no match.
        for (let j = 0; j < td.length; j++) {
            if (td[j]) {
                let textValue = td[j].innerText || td[j].textContent;
                textValue = limpaAcentos(textValue)
                if (textValue.toLowerCase().indexOf(filter) > -1) {
                    match = true;
                    break;
                }
            }
        }
        //esconde ou mostra linhas da tabela com base no match.
        tr[i].style.display = match ? "" : "none";
    }
}


async function teste(teste) {
    var cidadeIntra = teste.id;
    var button = teste;
    var url = "https://intra.mp.rs.gov.br/site/promotorias/" + cidadeIntra + "/relacao_equipamentos/";

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

        const conteudoDesejado = tempDiv.querySelector(".table");

        if (conteudoDesejado) {
            let targetDiv = button.parentElement.querySelector("#conteudo");
            const linhas = Array.from(conteudoDesejado.rows);

            // Adiciona as primeiras 5 linhas (ou menos, se não houver 5)
            for (let i = 2; i < linhas.length; i++) {
                if(linhas[i].innerHTML.includes("Descrição")){
                    break;
                }
                let buttonNew = document.createElement("button")
                buttonNew.innerHTML = "VERIFICAR";
                buttonNew.id = "t"+linhas[i].cells[2].innerHTML;
                buttonNew.onclick =  function() {
                    fetch(`/verificarTombo`, {
                        method: "POST",
                        body:  buttonNew.id,
                        headers: {
                            "Content-Type": "application/json"
                        }
                    })
                        .then(response => {
                            if (!response.ok) {
                                throw new Error('Erro na requisição: ' + response.statusText);
                            }
                            return response.text();
                        })
                        .then(data => {
                            linhas[i].cells[1].innerHTML = data;
                            alert(data)
                        })
                        .catch(error => {
                            linhas[i].cells[1].innerHTML = "PROVAVELMENTE DESLIGADO";
                            console.error('Erro:', error);
                        });

                };
                linhas[i].cells[3].appendChild(buttonNew)
                targetDiv.appendChild(linhas[i]);
            }

        } else {
            let targetDiv = button.parentElement.querySelector("#conteudo");
            targetDiv.innerHTML = "Sem contato na intra"
            console.log("Div não encontrada!");
        }
    } catch (error) {
        console.error("Erro ao carregar conteúdo:", error);
    }
}

function abreListaSW(){
    const btn = document.getElementById("btnListaSW")
    const list = document.getElementById("listOfSwitches")

    if(list.classList.contains("hideList")){
        btn.innerText = "Fechar Lista ˄";
    }else{
        btn.innerText = "Abrir Lista ˅";
    }

    list.classList.toggle("hideList");
}

async function getSwitchesDaUnidade(cidadeIntra) {
    var url = "https://intra.mp.rs.gov.br/site/promotorias/" + cidadeIntra + "/relacao_equipamentos/";

    try {
        const response = await fetch(url);
        const text = await response.text();
        const tempDiv = document.createElement("div");
        tempDiv.innerHTML = text;

        const table = tempDiv.querySelector("tbody");
        const switchTable = document.createElement('table');
        switchTable.classList.add("listOfSwitches", "hideList");
        switchTable.id = "listOfSwitches";

        const listaSwitches = document.getElementById("listaSwitches")

        if(table) {
            let afterOutros = false
            for(let i = 0; i < table.children.length; i++){
                if(afterOutros){
                    let tableLine = document.createElement('tr');

                    let switchTombo = document.createElement('td');
                    switchTombo.classList.add("switchTombo")

                    let switchName = document.createElement('td');
                    switchName.classList.add("switchName")

                    switchTombo.innerHTML = "<span style='color: #11bc7a;'>(" + table.children[i].children[2].innerText + ")</span>"
                    switchName.innerHTML = table.children[i].children[0].innerText

                    tableLine.appendChild(switchTombo)
                    tableLine.appendChild(switchName)

                    switchTable.appendChild(tableLine)
                }
                if(table.children[i].innerText == "Outros"){
                    afterOutros = true;
                }
            }

            listaSwitches.appendChild(switchTable)

        } else {
            console.log("Erro");
        }
    } catch (error) {
        console.error("Erro ao carregar conteúdo:", error);
    }


}





