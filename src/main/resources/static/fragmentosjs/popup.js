let url = '';

function abrirModal(acionador) {
    url = acionador.dataset.endpoint + '/' + acionador.dataset.id;
    abrir = acionador.dataset.abrir;
    let modalValor = document.getElementById("modalValor")
    if(abrir === "false"){
        if (acionador.dataset.dado !== undefined){
            modalValor.value = acionador.dataset.dado;
        }else{
            modalValor.value = acionador.dataset.id;
        }
        modalValor.readOnly = true;
        modalValor.style.display= "none";
    }
    else{
        modalValor.readOnly = false;
        modalValor.style.display= "flex";
        modalValor.value = "";

    }
    const modal = document.getElementById('inputModal')
    document.getElementById('titleModal').innerHTML = acionador.dataset.acao;
    document.getElementById('titleModal').style.color = "black";
    modal.style.display = "flex";

    modalValor.focus()
    document.addEventListener('keyup', enterEnvia, false);
    modal.onclick = (e) => { if(e.target === modal && e.code !== 'Enter') fecharModal();};
}

function enterEnvia(e) {
    if (e.code === 'Enter' || e.code === 'NumpadEnter') {
        enviarModal();
    }
    e.preventDefault();
    return false;
}

function fecharModal() {
    const modal = document.getElementById("inputModal");
    modal.style.display = "none";
    url = '';
    document.removeEventListener('keyup', enterEnvia, false);
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
            if (Array.isArray(data)) {
                let mensagemFormatada = data.join('<br>');
                showPopup("Lista de acompanhamentos: ", mensagemFormatada);
            } else {
                showPopup("Sucesso", data.message || data);
            }
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
        zIndex: "9999",
        color: "black"
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

    document.removeEventListener('keyup', enterEnvia, false);

    const closePopup = () => {
        if (popup) popup.remove();
        document.removeEventListener("click", closePopup);
        window.location.reload();
    };

    setTimeout(() => {
        document.addEventListener("click", closePopup);
        document.addEventListener("keyup", closePopup);
    }, 50);

    setTimeout(closePopup, 4000);
}