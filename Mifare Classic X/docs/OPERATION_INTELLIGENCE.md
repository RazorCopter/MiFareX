# P4 · Operation Intelligence

## Obiettivo

P4 rende osservabili le operazioni NFC senza trasformare la cronologia in un archivio di dati sensibili. Introduce audit locale, simulazione pre-scrittura e diagnostica del dispositivo.

## Audit locale

La tabella Room `operation_logs` registra:

- tipo, esito, origine e timestamp dell’operazione;
- vendor e riepilogo leggibile;
- durata e conteggi tecnici quando disponibili;
- solo le ultime quattro cifre esadecimali dell’UID, precedute da `••`.

Non registra mai chiavi MIFARE, payload, dump letti o UID completi. La cronologia conserva al massimo 1.000 eventi, resta sul dispositivo e può essere cancellata dall’utente senza modificare vendor o associazioni UID.

Gli eventi coperti sono scrittura manuale, scrittura Auto Mode, lettura, test chiavi, simulazione e UID Auto Mode non associato.

## Migrazione database

`AppDatabase` passa dalla versione 2 alla versione 3 con una migrazione esclusivamente additiva. Le tabelle `vendors` e `uid_entries` non vengono alterate; viene creata `operation_logs` con indici su timestamp, vendor, tipo ed esito.

## Simulazione pre-scrittura

`WritePlanAnalyzer` esegue un controllo locale e senza effetti collaterali. Calcola operazioni e settori coinvolti, verifica la presenza delle chiavi per i settori target e riusa le regole che proteggono manufacturer block, sector trailer e coordinate fuori range.

La simulazione non contatta alcun tag. Un piano non valido disabilita l’azione di armamento nella conferma di scrittura.

## Diagnostica NFC

La schermata Diagnostica mostra disponibilità e stato dell’adattatore, modello/API Android e controlli di sicurezza attivi. La compatibilità MIFARE Classic effettiva resta dipendente dal chipset e dal tag e viene verificata in modo non distruttivo tramite “Test chiavi”.

## Limiti intenzionali

- Nessun rollback automatico: richiederebbe conservare dati precedenti del tag e aumenterebbe il rischio di esposizione o ripristini non sicuri.
- Nessuna copia dei dump nella cronologia.
- Nessuna sincronizzazione cloud o telemetria remota.
- La diagnostica hardware non sostituisce un test con un tag reale.
