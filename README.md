# AutoGuardianGPS-Pro

Progetto Android compilabile automaticamente con GitHub Actions.

## Metodo semplice
1. Crea su GitHub un repository pubblico chiamato `AutoGuardianGPS-Pro`.
2. Carica TUTTI i file e le cartelle contenuti nello ZIP.
3. Esegui il commit.
4. Apri la scheda **Actions**.
5. Apri **Build AutoGuardian GPS Pro APK**.
6. Attendi il completamento con spunta verde.
7. Apri l'esecuzione completata.
8. In basso, nella sezione **Artifacts**, scarica `AutoGuardianGPS-Pro-debug`.
9. Estrai lo ZIP dell'artifact: dentro trovi `AutoGuardianGPS-Pro-debug.apk`.

## Nota importante
Questa versione base è realmente compilabile e mostra la posizione corrente del telefono dopo aver concesso il permesso GPS.

Le funzioni avanzate (geofence, rilevamento movimento, notifiche remote e posizione live da un secondo telefono) richiedono configurazione aggiuntiva e un backend/servizio di messaggistica.
