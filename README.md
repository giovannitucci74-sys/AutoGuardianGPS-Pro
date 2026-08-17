# AutoGuardian GPS Pro 4.0

Versione consolidata per Android 13 e successivi, con lo stesso `applicationId` e lo stesso progetto Firebase.

## Funzioni incluse

- telefono nascosto: servizio GPS in primo piano con aggiornamento ogni 10–15 secondi
- invio a Firebase di posizione, precisione, velocità, batteria e ultimo contatto
- telefono di controllo: mappa interna aggiornata automaticamente
- quattro comandi: Posizione auto, Attiva/Disattiva antifurto, Cronologia percorsi e Stato telefono nascosto
- indicazione online/non aggiornato, batteria e data dell’ultimo aggiornamento
- cronologia Firebase limitata automaticamente alle ultime 200 posizioni
- Viewer web separato, senza permessi Android sul telefono di controllo
- pulsante opzionale per aprire la posizione in Google Maps

Viewer operativo:
https://autoguardian-gps-pro.giovannitucci74.chatgpt.site/

## Compilazione APK

Il workflow GitHub Actions compila automaticamente la build Debug per ogni pull request verso `main`.
L’artifact generato si chiama `AutoGuardian-GPS-Pro-Debug` e contiene `app-debug.apk`.

## Installazione

1. Scaricare l’APK dall’artifact della build GitHub Actions.
2. Installarlo sul telefono nascosto e selezionare **TELEFONO NASCOSTO NELL’AUTO**.
3. Concedere posizione precisa, notifiche e autorizzazione al funzionamento in background.
4. Premere **ATTIVA TRACKER**.
5. Sul secondo telefono installare la stessa APK e selezionare **TELEFONO DI CONTROLLO**, oppure aprire il Viewer web.

## Privacy e sicurezza

Usare esclusivamente su un veicolo proprio o autorizzato. Android mostra una notifica persistente mentre il servizio GPS è attivo. Il telefono bancario può usare il Viewer web senza installare l’APK e senza concedere permessi sensibili.
