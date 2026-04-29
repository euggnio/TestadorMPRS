var eventSource = new EventSource("/stream");
var linha = {
    'nome': '@END',
    'velocidade': '',
};

eventSource.onmessage = function (event) {
    event.data = event.data.replace("'", "\"");
    let jsonData = event.data.replace("data:", "");
    let obj = JSON.parse(jsonData);
    if (obj.nome === "@END") {
        console.log("acabou teste")
        document.getElementById(linha.nome).classList.remove("text-shadows")
        document.getElementById("statusAtual").innerHTML = "PARADO";
        document.getElementById("buttonLigar").style.backgroundColor = "#00adb5";

        document.getElementById("loader").classList.remove("loader")
        if(linha.nome !== "@END"){
            getDataAtualizada(linha.nome);
        }
        linha.nome = "@END";
        document.getElementById("statusTeste").innerHTML = "Iniciar teste";

        linha.velocidade = "";
        return
    }
    if (linha.nome !== obj.nome) {
        console.log('Received troca de cidade: ' + linha.nome + " para " + obj.nome);
        if (linha.nome != "@END") {
            document.getElementById(linha.nome).classList.remove("text-shadows")

            getDataAtualizada(linha.nome);
        }
        linha = obj;
    }
    document.getElementById(linha.nome).classList.add("text-shadows")
    document.getElementById("statusAtual").innerHTML = "RODANDO";
    document.getElementById("statusTeste").innerHTML = "DESLIGAR";
    document.getElementById("buttonLigar").style.backgroundColor = "red"
    document.getElementById("loader").classList.add("loader")
    trocarDadosAtuais();
};

function getDataAtualizada(id){
    fetch(`/data?idCidade=${id}`, {
        method: "GET",
    })
        .then(response => response.text())
        .then(data => {
            let jsonData = data.replace("data:", "");
            let obj = JSON.parse(jsonData);
            console.log(obj);
            document.getElementById(id).getElementsByTagName('span')[2].innerHTML = obj.ultimoTesteBanda;
            document.getElementById(id).getElementsByTagName('span')[3].innerHTML = obj.dataUltimoTeste;
            document.getElementById(id).getElementsByTagName('span')[4].innerHTML = obj.check ? "OK" : "RUIM";
            document.getElementById(id).getElementsByTagName('span')[4].style.color = obj.check ? "GREEN" : "RED";

        });
}

function trocarDadosAtuais() {
    document.getElementById("cidadeAtual").innerHTML = linha.nome;
    document.getElementById("velocidadeAtual").innerHTML = linha.velocidade;
}

function iniciarTeste(idCidade) {
    fetch(`/testeBanda`, {
        method: "POST",
        body:  idCidade,
        headers: {
            "Content-Type": "application/json"
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Erro na requisição: ' + response.statusText);
            }
            return response.json(); // Pega a resposta JSON
        })
        .then(async data => {
            if (data.status == "true") {
                document.getElementById("statusTeste").innerHTML = "TESTE DESLIGADO."
            } else {
                document.getElementById("statusTeste").innerHTML = "INICIADO."
            }
        })
        .catch(error => {
            console.error('Erro:', error);
            alert("Desconectado, entre novamente");
            window.location.reload();

        });
}

function selectCity(){
    const params = new URLSearchParams(window.location.search);
    if(params.size == 0) return

    const cidadeFiltrar = params.get('cidade');
    const pesquisa = document.getElementById("search")

    pesquisa.value = cidadeFiltrar
    filterTable()
}
window.addEventListener('load', selectCity);