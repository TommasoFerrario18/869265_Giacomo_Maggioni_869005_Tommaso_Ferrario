function formattazioneJsono() {
}

document.getElementById("accountButton").addEventListener("click", () => {
    let accountID = document.getElementById("accountID").value;
    //console.log(accountID);

    let xhttp = new XMLHttpRequest()
    xhttp.onreadystatechange = () => {
        if (xhttp.readyState == 4 && xhttp.status == 200) {
            console.log(this.responseText);
        }
    }
    xhttp.open("GET", "http://localhost:8080/api/account")
    xhttp.send()
});

