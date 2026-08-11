# AutoGuardian GPS Pro — Debug Android 13+

Versione pronta per la prima prova su un telefono Android 13.

## La procedura più semplice

1. Crea su GitHub un repository pubblico chiamato `AutoGuardianGPS-Pro`.
2. Estrai questo ZIP sul PC.
3. Nel repository GitHub scegli **Add file → Upload files**.
4. Trascina nella pagina **tutto il contenuto** della cartella `AutoGuardianGPS-Pro`.
5. Premi **Commit changes**.
6. GitHub avvierà automaticamente la compilazione.
7. Quando la compilazione termina, nella pagina principale del repository apparirà **Releases**.
8. Apri l'ultima release e scarica `AutoGuardianGPS-Pro-debug.apk`.

Non serve Android Studio e non serve avviare manualmente GitHub Actions.

## Cosa fa questa build

- Android 13+ (`minSdk 26`, `targetSdk 35`)
- tracking GPS tramite Foreground Service
- aggiornamento GPS circa ogni 15 secondi
- ultima posizione salvata localmente
- pulsante ATTIVA TRACKING / DISATTIVA
- tentativo di ripristino del tracking dopo riavvio se era attivo
- servizio `START_STICKY` per maggiore resilienza

## Prima prova sul telefono

1. Installa l'APK.
2. Apri AutoGuardian GPS Pro.
3. Concedi il permesso di localizzazione e, se richiesto, notifiche.
4. Premi **ATTIVA TRACKING**.
5. Esci all'aperto o posiziona il telefono dove riceve bene il GPS.
6. Dopo 15–30 secondi premi **ULTIMA POSIZIONE**.

## Nota importante

Questa è la build Debug per verificare installazione e tracking locale. Geofence avanzato, allarme movimento remoto, mappa remota e fotografie evento non fanno ancora parte di questa build.

Usare esclusivamente su un veicolo proprio o autorizzato e rispettando i permessi/indicatori privacy di Android.
