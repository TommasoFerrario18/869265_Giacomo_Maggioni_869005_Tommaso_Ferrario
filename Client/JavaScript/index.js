function formattazioneJsono() {
}

function richiestaAccountID(url, method, callback) {
    let xhttp = new XMLHttpRequest()

    xhttp.onreadystatechange = () => {
        if (xhttp.readyState == 4 && xhttp.status == 200) {
            callback()
        }
    }

    xhttp.open(url, method)

    xhttp.send()
}

document.getElementById("accountButton").addEventListener("click", () => {
    let accountID = document.getElementById("accountID").value
});

