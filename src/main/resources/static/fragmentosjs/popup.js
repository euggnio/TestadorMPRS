let url = '';

function abrirModal(acionador) {
    url = acionador.dataset.endpoint + '/' + acionador.dataset.id;
    abrir = acionador.dataset.abrir;
    if(abrir === "false"){
        if (acionador.dataset.dado !== undefined){
            document.getElementById("modalValor").value = acionador.dataset.dado;
        }else{
            document.getElementById("modalValor").value = acionador.dataset.id;

        }
        document.getElementById("modalValor").readOnly = true;
        document.getElementById("modalValor").style.display= "none";
        alert(abrir)
    }
    else{
        document.getElementById("modalValor").readOnly = false;
        document.getElementById("modalValor").style.display= "flex";
        document.getElementById("modalValor").value = "";
    }
    const modal = document.getElementById('inputModal')
    document.getElementById('titleModal').innerHTML = acionador.dataset.acao;
    modal.style.display = "flex";

}

function fecharModal() {
    const modal = document.getElementById("inputModal");
    modal.style.display = "none";
    url = '';
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
        .then(async response => {
            const contentType = response.headers.get("content-type");
            let data;
            if (contentType && contentType.includes("application/json")) {
                data = await response.json();
            } else {
                data = await response.text();
            }

            if (!response.ok) {
                showPopup("Erro", data.message || data.error || "Erro desconhecido");
                return;
            }
            showPopup("Sucesso", data.message || data);
            setTimeout(() => window.location.reload(), 2000);
        })
        .catch(error => {
            showPopup("Erro de conexão", error.message);
        });
}

function showPopup(title, message) {
    const existing = document.getElementById("global-popup");
    if (existing) existing.remove();

    const popup = document.createElement("div");
    popup.id = "global-popup";

    popup.innerHTML = `
        <div class="popup-content">
            <h3>${title}</h3>
            <p>${message}</p>
        </div>
    `;

    Object.assign(popup.style, {
        position: "fixed",
        top: "0",
        left: "0",
        width: "100%",
        height: "100%",
        backgroundColor: "rgba(0,0,0,0.4)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: "9999"
    });

    const content = popup.querySelector(".popup-content");

    Object.assign(content.style, {
        background: "#fff",
        padding: "20px 30px",
        borderRadius: "8px",
        boxShadow: "0 5px 15px rgba(0,0,0,0.3)",
        textAlign: "center",
        minWidth: "250px",
        animation: "fadeIn 0.2s ease"
    });

    document.body.appendChild(popup);

    const closePopup = () => {
        if (popup) popup.remove();
        document.removeEventListener("click", closePopup);
    };

    setTimeout(() => {
        document.addEventListener("click", closePopup);
    }, 50);

    setTimeout(closePopup, 4000);
}