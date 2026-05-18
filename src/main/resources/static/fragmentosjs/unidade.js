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



window.addEventListener('load', () => {
    var eventSource = new EventSource("/stream");

    const cidadeAtual = document.getElementsByClassName("ligarTeste")[0].getAttribute('data-cidade')
    let ultimoNome = "@END"

    eventSource.onmessage = (event) => {
        let nomeAtual = JSON.parse(event.data).nome
        console.log(nomeAtual)

        if(ultimoNome == "@END" && nomeAtual == cidadeAtual){
            ultimoNome = cidadeAtual
        }

        if(ultimoNome == cidadeAtual && nomeAtual == "@END"){
            fetch(`/data?idCidade=${cidadeAtual}`, {
                    method: "GET",
                })
                    .then(response => response.text())
                    .then(data => {
                        let jsonData = data.replace("data:", "");
                        let obj = JSON.parse(jsonData);
                        console.log(obj.ultimoTesteBanda);

                        let okElem = document.createElement("span")

                        document.getElementById("ultimo-teste").innerHTML = obj.ultimoTesteBanda + "&nbsp;&nbsp;&nbsp;"
                        okElem.style.color = obj.check ? "green" : "red"
                        okElem.innerHTML = obj.check ? "OK ✅" : "NÃO OK"

                        document.getElementById("ultimo-teste").appendChild(okElem)
                    });

            ultimoNome = "@END"
        }
    }
});
