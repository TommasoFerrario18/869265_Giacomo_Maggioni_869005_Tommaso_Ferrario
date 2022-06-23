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
- /api/account: 
- /api/account/{accountId}
- /api/transfer
- /api/divert 

È possibile ottenere anche delle pagine HTML effettuando una richiesta di tipo GET agli endpoint:
- /
- /transfer

I dati ritornati dagli endpoint REST sono in formato JSON.
