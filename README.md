# AutoGuardian GPS Pro — Debug Android

Progetto Android pronto per compilazione automatica con GitHub Actions.

## Caricamento corretto su GitHub

Dopo l'upload, nella pagina principale del repository devono comparire direttamente:

- `.github/`
- `app/`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`

**Non** deve esserci una cartella esterna `AutoGuardianGPS-Pro/` che racchiude questi file.

Il workflow `.github/workflows/build-debug.yml` installa Java 17, Android SDK 35 e Gradle 8.10.2, quindi non richiede il Gradle Wrapper nel repository.

## Download APK

Dopo il commit:

1. apri **Actions** e controlla `Build AutoGuardian Debug APK`;
2. quando il job è verde, apri **Releases**;
3. scarica `AutoGuardianGPS-Pro-debug.apk`.

## Funzioni della build Debug

- tracking GPS tramite foreground service;
- aggiornamento posizione circa ogni 15 secondi;
- ultima posizione salvata localmente;
- attivazione/disattivazione manuale;
- tentativo di riavvio del servizio dopo il boot se il tracking era attivo.

Questa prima build serve a verificare installazione e tracking sul dispositivo. Usare solo su veicoli propri o con autorizzazione.
