# 869265_Giacomo_Maggioni_869005_Tommaso_Ferrario
Progetto Sistemi Distribuiti 2021-2022 Sistema bancario.  

Membri del gruppo:
  -
- 869265 Giacomo Maggioni
- 869005 Tommaso Ferrario

Sistema bancario
  -
Lo scopo di questa applicazione è quello di implementare delle funzioni elementari relative a dei servizi bancari, come:
- Creazione di un account
- Chiusura di un account
- Versamento e prelievo di denaro
- Spostamento di denaro da un account ad un altro, non necessariamente dello stesso proprietario

Queste funzionalità vengono fornite attraverso degli endpoint REST:
  -
/api/account: 
  -
* `GET`: restituisce la lista di tutti gli account nel sistema.
* `POST`: crea un nuovo account con i seguenti campi: name, surname e ritorna nel body della risposta il nuovo id dell'account creato.
* `DELETE`: elimina l'account con id specificato dal parametro URL id.

/api/account/{accountId}:
  - 
* `GET`: restituisce il nome e cognome del proprietario nonché il saldo con un elenco degli identificativi di tutte le transazioni effettuate da accountId, in ordine cronologico ascendente. Inoltre, introduce un header di risposta con chiave X-Sistema-Bancario. Il valore dell’header deve esprimere il nome e cognome del proprietario in formato nome;cognome.
* `POST`: effettua un versamento di denaro con un importo specificato dalla chiave amount nel body della richiesta. Se amount è negativo, viene eseguito un prelievo. In caso di successo, nel body della rispostaviene restituito il nuovo saldo del conto ed un identificativo del versamento/prelievo in formato UUID v4.
* `PUT`: modifica name e surname del proprietario del conto. Nel body devono quindi essere presenti le seguenti chiavi: name, surname.
* `PATCH`: modifica name oppure surname del proprietario del conto. Nel body deve quindi essere presente solamente una tra le seguenti chiavi: name, surname
* `HEAD`: restituisce nome e cognome del proprietario in un header di risposta con chiave X-Sistema-Bancario.

/api/transfer:
  -
* `POST`: effettua uno spostamento di denaro con amount positivo da un account a un altro. amount è specificato nel body della richiesta. Il body della richiesta presenta quindi i seguenti campi: from, to, amount

/api/divert: 
  -
* `POST`: annulla una transazione con id specificato dalla chiave id nel body della richiesta.

È possibile ottenere anche delle pagine HTML effettuando una richiesta di tipo GET agli endpoint:
- /
- /transfer

I dati ritornati dagli endpoint REST sono in formato JSON.
