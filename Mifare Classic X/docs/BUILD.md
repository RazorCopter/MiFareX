# Build e qualita'

## Prerequisiti

- JDK 21 (Gradle 9.4.1 usa il wrapper incluso nel repository).
- Android SDK con piattaforma `android-35` e Build Tools compatibili.
- Nessun Gradle installato globalmente: usare sempre `gradlew`/`gradlew.bat`.

## Comandi locali

Da questa directory:

```powershell
.\gradlew.bat --dependency-verification=strict testDebugUnitTest lintDebug jacocoTestReport assembleDebug
```

I report di lint sono in `app/build/reports/lint-results-debug.html`; la copertura
JaCoCo e' in `app/build/reports/jacoco/jacocoTestReport/html/index.html` e in formato
XML per gli strumenti CI. L'APK debug e' in
`app/build/outputs/apk/debug/app-debug.apk`.

I test strumentali richiedono un dispositivo/emulatore connesso:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## Dipendenze riproducibili

`app/gradle.lockfile` fissa le versioni risolte e
`gradle/verification-metadata.xml` verifica gli artefatti tramite SHA-256. La CI usa
la verifica stretta e fallisce se una dipendenza non e' bloccata o verificata.

Per aggiornare intenzionalmente le dipendenze, modificare prima `app/build.gradle`,
rieseguire i controlli di sicurezza dell'aggiornamento e poi generare entrambi i file:

```powershell
.\gradlew.bat help --write-locks --write-verification-metadata sha256
```

Revisionare e includere nel commit le modifiche a lockfile e metadata: non eliminarli
per aggirare un errore di verifica.

## CI e segreti

`.github/workflows/ci.yml` esegue wrapper validation, test unitari, lint, copertura,
build debug e scansione Gitleaks del contenuto corrente del checkout. La scansione non
riscrive la cronologia Git: eventuali segreti gia' pubblicati nella storia richiedono
rotazione e una procedura di bonifica separata.

Le due proprieta' di compatibilita' in `gradle.properties` restano necessarie finche'
i plugin legacy non saranno migrati dalle API `BaseExtension` e dal plugin Kotlin
esterno alle API AGP correnti.
