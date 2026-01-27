let url = '';

function abrirModal(acionador) {
    url = acionador.dataset.endpoint + '/' + acionador.dataset.id;
    const modal = document.getElementById('inputModal')
    document.getElementById('titleModal').innerHTML = acionador.dataset.acao;
    modal.style.display = "flex";
}

function fecharModal() {
    const modal = document.getElementById("inputModal");
    modal.style.display = "none";
}

function enviarModal() {
    const valor = document.getElementById("modalValor").value;
    document.getElementById("modalValor").value = '';
    const modal = document.getElementById("inputModal");
    modal.style.display = "none";

    fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: valor
    })
        .then(response => {
            if (!response.ok) {
                throw new Error("Erro na requisição");
            }
            window.location.reload();
        })
        .then(data => {
            console.log("Resposta do backend:", data);
        })
        .catch(error => {
            console.error("Erro:", error);
        });
}