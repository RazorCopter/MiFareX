# MiFareClassicX — Report Tecnico Completo

> **Versione documento**: 2.0 — Ultimo aggiornamento: 29 Giugno 2026
> **Stato**: Implementazione Fase 0-4 completata, build verification pendente
> **Autore**: Senior Mobile Architect / AI Agent Session

---

## PARTE 1 — STATO ORIGINALE (LEGACY)

### 1.1 Panoramica Applicazione Originale

| Proprietà | Valore |
|-----------|--------|
| **Tipo App** | Android nativa |
| **Linguaggio** | Java (plugin Kotlin configurato ma non usato) |
| **Package** | `de.syss.MifareClassicTool` |
| **Versione originale** | 4.3.1 (versionCode 70) |
| **minSdk originale** | 19 (Android 4.4) |
| **targetSdk** | 35 (Android 15) |
| **Tema** | `Theme.AppCompat.DayNight` |
| **Dipendenze** | `androidx.core`, `androidx.preference`, `androidx.appcompat` |
| **Build system** | Gradle con Android Gradle Plugin 9.2.0, Kotlin 2.2.10 |
| **Scopo** | Tool per leggere, scrivere, analizzare e manipolare tag NFC MIFARE Classic |

### 1.2 Architettura Legacy

- **Pattern**: Nessun Fragment, nessun ViewModel, nessun Navigation Component
- **Ogni schermata** = una Activity autonoma (12+ Activity)
- **Navigation**: Intent espliciti tra Activity
- **State**: `Common.java` come singleton `Application` (stato globale condiviso)
- **Layout**: XML tradizionale (RelativeLayout, LinearLayout, ScrollView)
- **Zero**: Jetpack Compose, data binding, RecyclerView, Material Design 3

### 1.3 Struttura Cartelle Originale

```
MiFareClassicX/
├── metadata/                              # F-Droid / store metadata
│   ├── en-US/changelogs/                  # Changelog v57→70
│   └── en-US/images/phoneScreenshots/     # 8 screenshot ufficiali
│
└── Mifare Classic X/                      # PROGETTO ANDROID
    ├── app/build.gradle                   # Build config
    ├── settings.gradle                    # Plugin management
    ├── gradle.properties                  # Properties (android.useAndroidX=true)
    └── app/src/main/
        ├── AndroidManifest.xml
        ├── java/de/syss/MifareClassicTool/
        │   ├── Common.java                # Singleton Application: stato globale
        │   ├── MCReader.java              # Engine NFC MIFARE Classic (51KB)
        │   ├── MCDiffUtils.java           # Utilità diff dump
        │   └── Activities/                # 12+ Activity Java
        ├── res/
        │   ├── layout/                    # 21 layout XML + 5 dialog/list
        │   ├── menu/                      # 8 menu XML
        │   ├── values/                    # strings, colors, styles
        │   ├── drawable/                  # Icone PNG per bottoni
        │   ├── mipmap-*/                  # Icone launcher
        │   └── xml/                       # NFC filter (nfc_tech_filter.xml)
        └── assets/
            ├── key-files/                 # std.keys, extended-std.keys
            └── help/help.html             # Documentazione inline
```

### 1.4 Classi Core Legacy (NON MODIFICATE)

#### `Common.java` — Singleton Application

- `NfcAdapter`, `Tag`, `MifareClassic` corrente
- `useAsEditorOnly` flag per modalità senza NFC
- Metodi NFC foreground dispatch
- File I/O helpers (`getFile`, `readFileLineByLine`, `saveFile`)
- `hex2Bytes()`, `bytes2Hex()` — conversione hex
- Color-coding helpers per Dump Editor
- `treatAsNewTag()` — gestione intent NFC in arrivo

#### `MCReader.java` — Engine NFC (51KB, 1274 linee)

- `get(Tag)` → restituisce `MCReader` o `null` se non MIFARE Classic
- `connect()` / `close()` / `isConnected()` — lifecycle connessione (timeout 500ms)
- `authenticate(sectorIndex, key, useAsKeyB)` — **PRIVATE** con retry logic
- `writeBlock(sector, block, data, key, useKeyB)` → return code (0=OK, 1-4=errore, -1=I/O)
- `writeValueBlock(sector, block, value, increment, key, useKeyB)` → same codes
- `readAsMuchAsPossible(keyMap)` → `SparseArray<String[]>` dei settori letti
- `getSectorCount()` / `getBlockCount()` / `getBlockCountInSector()`
- `setKeyFile()` / `setMappingRange()` / `buildNextKeyMapPart()` — key mapping
- `valueTransferRestore()` — operazioni value block avanzate

#### `MCDiffUtils.java` — Diff tra dump

- Confronto blocco per blocco
- Produce lista differenze annotate

### 1.5 Mappa Completa Activity Legacy

| # | Activity | Funzione |
|---|----------|----------|
| 1 | `MainMenu` | Entry point: griglia 2×3 di bottoni (Read/Write/Edit/Keys/Tools/Help) |
| 2 | `ReadTag` | Schermata attesa lettura NFC → naviga a DumpEditor |
| 3 | `WriteTag` | 5 modalità: Write Block, Write Dump, Clone UID, Factory Format, Value Block |
| 4 | `DumpEditor` | Editor hex per dump (genera EditText dinamicamente, color-coding) |
| 5 | `KeyEditor` | Editor file chiavi (.keys) |
| 6 | `KeyMapCreator` | Mapping chiave→settore con progress |
| 7 | `FileChooser` | Selezione file dumps/keys |
| 8 | `TagInfoTool` | Info tag (UID, ATQA, SAK, tipo, memoria) |
| 9 | `AccessConditionTool` | Encoder Access Conditions |
| 10 | `AccessConditionDecoder` | Decoder Access Conditions |
| 11 | `ValueBlockTool` | Encoder/Decoder Value Blocks |
| 12 | `ValueBlocksToInt` | Conversione Value Blocks → interi |
| 13 | `DiffTool` | Confronto due dump |
| 14 | `BccTool` | Calcola BCC da UID |
| 15 | `CloneUidTool` | Clona UID su Magic Tag |
| 16 | `HexToAscii` | Conversione hex ↔ ASCII |
| 17 | `DataConversionTool` | Conversione dati multi-formato |
| 18 | `ImportExportTool` | Import/Export dump (legacy) |
| 19 | `UidLogTool` | Log UID nel tempo |
| 20 | `HelpAndInfo` | WebView con help.html |
| 21 | `Preferences` | Impostazioni app |

---

## PARTE 2 — OBIETTIVO TRASFORMAZIONE v2.0

### 2.1 Visione

Trasformare da "tool tecnico per smanettoni" a **piattaforma moderna** con 3 macro-sezioni:

1. **Modalità Operativa (User Mode)**: Griglia vendor → tap → NFC auto-write
2. **Modalità Configura (Admin Mode)**: Editor vendor (chiavi + payload) + Import/Export JSON
3. **Modalità Expert**: Shell per i 16 tool legacy (Activity Java intatte)

### 2.2 Decisioni Architetturali Approvate

| Decisione | Scelta | Motivazione |
|-----------|--------|-------------|
| **UI Framework** | Jetpack Compose (Kotlin) | Integrazione nativa NFC, nessun bridge overhead |
| **minSdk** | 24 (Android 7.0) | Requisito Compose |
| **Storage** | Room Database | Vendor con chiavi/payload come JSON |
| **Crittografia DB** | NO (Room in chiaro) | Target uso personale, complessità non giustificata |
| **Multi-profilo** | 1 Vendor = 1 Profilo | Keep it simple |
| **Icone Vendor** | Material Icons built-in | MVP, custom picker in futuro |
| **Tema Legacy** | Intatto (AppCompat) | Activity Expert Mode invariate |
| **Codice Legacy** | ZERO modifiche ai 24 file Java | Integrazione via Intent + NfcBridge wrapper |

---

## PARTE 3 — ARCHITETTURA v2.0 IMPLEMENTATA

### 3.1 Stack Tecnologico

| Componente | Libreria | Versione |
|------------|----------|----------|
| Build | AGP | 9.2.0 |
| Kotlin | kotlin-android | 2.2.10 |
| Compose Compiler | kotlin-plugin-compose | 2.2.10 |
| Compose BOM | compose-bom | 2025.06.01 |
| Material 3 | material3 | (BOM) |
| Navigation | navigation-compose | 2.9.0 |
| Room | room-runtime/ktx | 2.7.1 |
| KSP | ksp | 2.2.10-1.0.32 |
| Serialization | kotlinx-serialization-json | 1.8.1 |
| Coil | coil-compose | 2.7.0 |
| Coroutines | kotlinx-coroutines-android | 1.10.2 |
| Lifecycle | lifecycle-runtime/viewmodel-compose | 2.9.1 |
| Activity | activity-compose | 1.10.1 |

### 3.2 Struttura Package Nuova (Kotlin)

```
java/de/syss/MifareClassicTool/
│
│   # === LEGACY (INTATTO) ===
├── Common.java                    # Singleton Application
├── MCReader.java                  # Engine NFC
├── MCDiffUtils.java               # Diff utility
├── package-info.java
├── Activities/                    # 21 Activity Java
│
│   # === NUOVO (KOTLIN) ===
├── bridge/                        # Integrazione Legacy ↔ Compose
│   ├── NfcBridge.kt              # Wrapper MCReader con pre-flight + coroutines
│   └── LegacyInterop.kt         # Intent launcher per 16 Activity legacy
│
├── data/
│   ├── model/                     # Data classes
│   │   ├── Enums.kt              # VendorCategory, TagType, WriteResult, WriteMode
│   │   ├── SectorKey.kt          # Chiave A/B per settore (con validazione hex)
│   │   ├── PayloadConfig.kt     # WriteBlockEntry, ValueBlockOp, PayloadConfig
│   │   ├── VendorEntity.kt      # Room Entity (keys/payload come JSON string)
│   │   └── VendorConfig.kt      # JSON import/export model + VendorExportBundle
│   ├── db/
│   │   ├── AppDatabase.kt       # Room Database singleton (mctx.db)
│   │   ├── VendorDao.kt         # DAO con Flow + snapshot queries
│   │   └── Converters.kt        # TypeConverters per enum
│   └── repository/
│       └── VendorRepository.kt  # CRUD + Entity↔Config + JSON serialize
│
├── domain/
│   └── model/
│       └── WriteOperationResult.kt # PreflightResult (8 varianti) + BlockWriteResult + WriteOperationResult (4 varianti)
│
└── ui/
    ├── ComposeActivity.kt        # NUOVO LAUNCHER — NFC foreground dispatch
    ├── theme/
    │   ├── Color.kt              # Palette teal/amber/violet + status colors
    │   ├── Typography.kt         # Material 3 type scale
    │   └── Theme.kt              # MctxTheme con dynamic color Android 12+
    ├── navigation/
    │   └── MctxNavGraph.kt       # Routes + bottom nav + animated transitions
    ├── components/
    │   └── VendorCard.kt         # Card con gradient icon + status badge
    ├── usermode/
    │   ├── VendorGridScreen.kt   # Griglia vendor 2 colonne + search + empty state
    │   ├── VendorGridViewModel.kt # Flow con flatMapLatest per search
    │   ├── VendorDetailScreen.kt # Dettaglio + 4 overlay (Waiting/Verifying/Writing/Result)
    │   └── VendorWriteViewModel.kt # State machine NFC write (5 stati)
    ├── adminmode/
    │   ├── VendorEditorScreen.kt # Form create/edit con sezioni chiavi + payload
    │   ├── VendorEditorViewModel.kt # Form state management
    │   ├── ImportExportScreen.kt # SAF con CreateDocument + OpenDocument
    │   └── ImportExportViewModel.kt # ContentResolver I/O su Dispatchers.IO
    └── expertmode/
        └── ExpertModeScreen.kt   # Griglia 16 tool legacy con Intent launch
```

### 3.3 Diagramma Architetturale

```
┌─────────────────────────────────────────────────────┐
│                 ComposeActivity.kt                    │
│          (NFC Foreground Dispatch + Compose Host)     │
├─────────────────────────────────────────────────────┤
│ MctxNavGraph — Bottom Navigation (3 tab)             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐          │
│  │ Operativo│  │Configura │  │  Expert  │          │
│  │(UserMode)│  │(AdminMode│  │  (Legacy)│          │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘          │
│       │              │              │                 │
│  VendorGrid     VendorEditor   ExpertMode           │
│       │              │         Screen               │
│  VendorDetail   ImportExport       │                 │
│       │              │              │                 │
├───────┼──────────────┼──────────────┼─────────────────┤
│       │              │              │   BRIDGE LAYER  │
│  NfcBridge.kt   VendorRepo    LegacyInterop.kt     │
│  (Pre-flight    (Room+JSON)    (Intent launcher)    │
│   + Write)           │              │                 │
├───────┼──────────────┼──────────────┼─────────────────┤
│       │              │              │   LEGACY JAVA   │
│  MCReader.java  AppDatabase   16 Activity.java      │
│  Common.java    VendorDao     (WriteTag, ReadTag,   │
│                 VendorEntity   DumpEditor, ecc.)    │
└─────────────────────────────────────────────────────┘
```

---

## PARTE 4 — DETTAGLIO IMPLEMENTAZIONI CRITICHE

### 4.1 NFC Write Flow "Bulletproof" con Pre-flight Check

**State machine (5 stati):**

```
Idle → WaitingForTag → Verifying → Writing → Completed
                       ↘ (fail) ──────────→ Completed(PreflightFailed)
```

**Pre-flight checks (in ordine):**

| # | Check | Dove | Errore |
|---|-------|------|--------|
| 1 | Chiavi configurate? | `VendorWriteViewModel.startWriteFlow()` | `NoKeysConfigured` |
| 2 | Payload configurato? | `VendorWriteViewModel.startWriteFlow()` | `NoPayloadConfigured` |
| 3 | Tag è MIFARE Classic? | `NfcBridge.executeVendorWriteWithPreflight()` | `TagNotSupported` |
| 4 | Settori tag ≥ settori attesi? | `NfcBridge.runPreflight()` | `TagTypeMismatch` |
| 5 | Auth chiavi su ogni settore target | `NfcBridge.runPreflight()` | `KeyAuthFailed(sectors)` |
| 6 | Tag ancora presente? | catch `TagLostException` | `TagLost` |

**Strategia autenticazione pre-flight:**

- Usa `MCReader.readAsMuchAsPossible(miniKeyMap)` settore per settore
- Se il risultato è null o vuoto → chiave errata per quel settore
- Raccoglie tutti i settori falliti prima di abortire

**Strategia scrittura (dopo pre-flight OK):**

- Per ogni blocco: tenta Key B → se fallisce → fallback Key A
- Estratta in `tryWriteWithFallback()` riusabile
- Skip automatico manufacturer block (settore 0, blocco 0) se non esplicitamente abilitato

**File chiave:**

- `NfcBridge.kt` — 310 righe, metodi pubblici: `executeVendorWriteWithPreflight()`, `runPreflightOnly()`
- `WriteOperationResult.kt` — `PreflightResult` sealed class con 8 varianti, `WriteOperationResult` con 4 varianti

### 4.2 SAF Import/Export JSON

**Nessun permesso di storage necessario** — SAF (Storage Access Framework) garantisce accesso per-URI.

| Operazione | Android API | MIME Type |
|------------|-------------|-----------|
| Export | `ActivityResultContracts.CreateDocument("application/json")` | `application/json` |
| Import | `ActivityResultContracts.OpenDocument()` | `["application/json", "text/plain"]` |

**Flusso Export:**

1. User tap → SAF file picker → user sceglie posizione
2. `ImportExportViewModel.exportToUri(uri)` → `VendorRepository.exportAllToJson()`
3. `ContentResolver.openOutputStream(uri)` → write UTF-8 su `Dispatchers.IO`
4. File suggerito: `mctx_vendors_YYYYMMDD_HHmm.json`

**Flusso Import:**

1. User tap → SAF file picker → user seleziona file
2. `ImportExportViewModel.importFromUri(uri)` → `ContentResolver.openInputStream(uri)`
3. Validazione: non vuoto + inizia con `{`
4. `VendorRepository.importFromJson(json)` → deserialize + insertVendors
5. Gestione `SerializationException` per file malformati

**Schema JSON (VendorExportBundle):**

```json
{
  "version": 1,
  "exportedAt": 1751184000000,
  "vendors": [
    {
      "id": "uuid",
      "name": "Autolavaggio Mario",
      "subtitle": "Via Roma 42",
      "category": "CAR_WASH",
      "tagType": "MIFARE_CLASSIC_1K",
      "keys": [
        { "sector": 1, "keyA": "FFFFFFFFFFFF", "keyB": "FFFFFFFFFFFF" }
      ],
      "payload": {
        "writeMode": "SELECTIVE_BLOCKS",
        "blocks": [
          { "sector": 1, "block": 0, "data": "00112233445566778899AABBCCDDEEFF" }
        ]
      }
    }
  ]
}
```

### 4.3 Room Database Schema

**Database**: `mctx.db` (Room, versione 1, `fallbackToDestructiveMigration`)

**Tabella `vendors`:**

| Colonna | Tipo | Note |
|---------|------|------|
| `id` | TEXT (PK) | UUID |
| `name` | TEXT | Nome vendor |
| `subtitle` | TEXT? | Sottotitolo opzionale |
| `iconUri` | TEXT? | URI icona (futuro) |
| `category` | TEXT | Enum: CAR_WASH, GYM, VENDING, ACCESS_CONTROL, PARKING, CUSTOM |
| `notes` | TEXT? | Note libere |
| `tagType` | TEXT | Enum: MIFARE_CLASSIC_1K, MIFARE_CLASSIC_4K, MIFARE_CLASSIC_MINI |
| `keysJson` | TEXT | JSON array di SectorKey |
| `payloadJson` | TEXT | JSON object PayloadConfig |
| `createdAt` | INTEGER | Timestamp creazione |
| `updatedAt` | INTEGER | Timestamp ultimo update |
| `lastWriteResult` | TEXT | Enum: SUCCESS, PARTIAL, FAILED, NEVER_USED |
| `writeCount` | INTEGER | Contatore scritture |
| `sortOrder` | INTEGER | Ordinamento nella griglia |

### 4.4 Material 3 Theme

- **Primary**: Deep Teal/Cyan (`#006A6A` light / `#80D5D4` dark)
- **Secondary**: Warm Amber (`#8B5000` light / `#FFB870` dark)
- **Tertiary**: Violet (`#6750A4` light / `#CFBCFF` dark)
- **Dynamic Color**: Abilitato su Android 12+ con fallback alla palette curata
- **Typography**: Scale M3 con pesi SemiBold/Medium per titoli

---

## PARTE 5 — FILE MODIFICATI (LEGACY)

### 5.1 `app/build.gradle`

**Modifiche:**

- `minSdk`: 19 → **24**
- `versionCode`: 70 → **71**
- `versionName`: "4.3.1" → **"5.0.0-alpha01"**
- **Plugin aggiunti**: `kotlin-plugin-compose`, `kotlin-plugin-serialization`, `ksp`
- **Build features**: `compose true`
- **compileOptions/kotlinOptions**: Java 17 + JVM target 17
- **Dipendenze aggiunte**: Compose BOM, Material 3, Navigation, Room + KSP, Serialization, Coil, Coroutines, Lifecycle, Activity-Compose

### 5.2 `AndroidManifest.xml`

**Modifiche:**

- `ComposeActivity` aggiunta come **nuovo LAUNCHER** con `@style/ComposeTheme`
- `MainMenu` declassata a `exported="false"` (accessibile solo da Expert Mode)
- `activity-alias` NFC (`TECH_DISCOVERED`) retargetizzata a `ComposeActivity`

### 5.3 `res/values/styles.xml`

**Aggiunta:** `ComposeTheme` (parent `Theme.AppCompat.DayNight.NoActionBar`) per ComposeActivity

---

## PARTE 6 — STATO COMPLETAMENTO

### 6.1 Task Tracker

| Fase | Descrizione | Stato |
|------|-------------|-------|
| **Fase 0** | Infrastructure (build.gradle, package structure, Room, theme) | ✅ Completa |
| **Fase 1** | User Mode (VendorGrid, VendorDetail, NFC flow) | ✅ Completa |
| **Fase 2** | Admin Mode (VendorEditor, ImportExport SAF) | ✅ Completa |
| **Fase 3** | Expert Mode (Legacy tool grid + Intent launcher) | ✅ Completa |
| **Fase 4** | Hardening (Pre-flight check, error handling, SAF I/O) | ✅ Completa |
| **Build** | `./gradlew assembleDebug` | ⏳ Pendente |
| **Test** | Test su device fisico con tag MIFARE Classic | ⏳ Pendente |

### 6.2 Conteggio File

| Categoria | File | Note |
|-----------|------|------|
| Nuovi file Kotlin | **24** | Creati da zero |
| File legacy modificati | **3** | build.gradle, Manifest, styles.xml |
| File legacy Java intatti | **24** | Zero modifiche |
| File legacy res intatti | **35+** | Layout, menu, drawable, strings |

### 6.3 Rischi e TODO Aperti

| Rischio/TODO | Priorità | Dettaglio |
|--------------|----------|-----------|
| **Build verification** | 🔴 Alta | `./gradlew assembleDebug` non ancora eseguito |
| **Compilazione NfcBridge** | 🔴 Alta | Verifica che `MCReader.readAsMuchAsPossible()` accetti il formato `SparseArray` |
| **MCReader.authenticate() è private** | 🟡 Media | Il pre-flight usa `readAsMuchAsPossible()` come proxy di auth — funziona ma è un workaround |
| **Typography import** | 🟡 Media | `Typography.kt` importa `googlefonts.Font` ma non lo usa — rimuovere import morto |
| **Coil non usato** | 🟢 Bassa | Aggiunto per future icone vendor, attualmente inutilizzato |
| **exportAllToJson()** | 🟢 Bassa | Fix applicato, usa `getAllVendorsSnapshot()` |
| **Room migration** | 🟢 Bassa | Attualmente `fallbackToDestructiveMigration()` — OK per alpha |
| **Input validation UX** | 🟡 Media | Hex format/length validation nel VendorEditorScreen non implementata |
| **Lottie animations** | 🟢 Bassa | Non implementate, miglioramento futuro UX |

---

## PARTE 7 — GUIDA PER AGENTE SUCCESSIVO

### 7.1 Path Workspace

```
Workspace root: c:\Users\gianvito.bleve\OneDrive - Banca Mediolanum SPA\Documenti\Progetti\MiFareClassicX\
Progetto Android: Mifare Classic X\
Build file: Mifare Classic X\app\build.gradle
Manifest: Mifare Classic X\app\src\main\AndroidManifest.xml
Sorgenti Java legacy: Mifare Classic X\app\src\main\java\de\syss\MifareClassicTool\
Sorgenti Kotlin nuovi: Mifare Classic X\app\src\main\java\de\syss\MifareClassicTool\{bridge,data,domain,ui}\
```

### 7.2 Comandi Utili

```bash
# Build debug
cd "Mifare Classic X"
./gradlew assembleDebug

# Clean build
./gradlew clean assembleDebug

# Check dipendenze
./gradlew :app:dependencies
```

### 7.3 Convenzioni Codice

- **Package**: `de.syss.MifareClassicTool` (uguale al legacy — condividono lo stesso namespace)
- **Lingua UI**: Italiano (testi hardcoded — TODO: estrarre in strings.xml)
- **Naming**: Screen → ViewModel → Repository → Dao pattern
- **Coroutines**: Tutto il NFC I/O su `Dispatchers.IO` via `withContext`
- **State**: `StateFlow` per ViewModel → `collectAsStateWithLifecycle` in Compose
- **Legacy bridge**: MAI modificare file Java — wrapper Kotlin + Intent

### 7.4 Prossimi Passi Suggeriti

1. **Build verification** — Eseguire `./gradlew assembleDebug` e risolvere errori di compilazione
2. **Test su device** — Deploy su telefono con NFC + tag MIFARE Classic
3. **Validazione input** — Hex format/length checks nel VendorEditorScreen
4. **Estrarre stringhe** — Spostare testi italiani in `strings.xml` per i18n
5. **Icona launcher** — Aggiornare icona per riflettere il rebrand v2.0
6. **ProGuard rules** — Aggiungere keep per kotlinx-serialization se minify abilitato
7. **Lottie** — Sostituire le animazioni CSS con Lottie per overlay NFC

### 7.5 File Critici da Conoscere

| Scopo | File |
|-------|------|
| Entry point app | `ui/ComposeActivity.kt` |
| Logica NFC write | `bridge/NfcBridge.kt` |
| State machine write | `ui/usermode/VendorWriteViewModel.kt` |
| UI write flow | `ui/usermode/VendorDetailScreen.kt` |
| Database config | `data/db/AppDatabase.kt` |
| Schema dati | `data/model/VendorEntity.kt` |
| JSON I/O | `data/repository/VendorRepository.kt` |
| SAF file picker | `ui/adminmode/ImportExportViewModel.kt` |
| Legacy tool launcher | `bridge/LegacyInterop.kt` |
| MCReader Java (leggere, non modificare) | `MCReader.java` |

---

## PARTE 8 — MAPPA ACTIVITY LEGACY (RIFERIMENTO)

### 8.1 Entry Point — MainMenu (DECLASSATA)

- **File**: `Activities/MainMenu.java`
- **Layout**: `res/layout/activity_main_menu.xml`
- **Stato v2**: Non più launcher, accessibile da Expert Mode
- **UI**: Griglia 2×3 di Button con icona sopra il testo

### 8.2 Strumenti (tutti accessibili da ExpertModeScreen)

| Activity | Funzione |
|----------|----------|
| `ReadTag` | Lettura NFC → DumpEditor |
| `WriteTag` | 5 modalità scrittura |
| `DumpEditor` | Editor hex dump (UI generata dinamicamente in Java) |
| `KeyEditor` | Editor file .keys |
| `TagInfoTool` | Info tag (UID, ATQA, SAK) |
| `AccessConditionTool` | Encoder AC |
| `AccessConditionDecoder` | Decoder AC |
| `ValueBlockTool` | Encoder/Decoder Value Blocks |
| `ValueBlocksToInt` | VB → interi |
| `DiffTool` | Diff tra dump |
| `BccTool` | Calcola BCC |
| `CloneUidTool` | Clona UID su Magic Tag |
| `HexToAscii` | Hex ↔ ASCII |
| `DataConversionTool` | Multi-formato |
| `ImportExportTool` | Import/Export dump (legacy) |
| `UidLogTool` | Log UID |

### 8.3 Note Importanti su MCReader

- `MCReader.authenticate()` è **PRIVATE** — il bridge non può chiamarlo direttamente
- Il pre-flight usa `readAsMuchAsPossible(keyMap)` come proxy per verificare l'autenticazione
- `MCReader.get(tag)` ritorna `null` se il tag non è MIFARE Classic — primo guardrail
- `MCReader.connect()` ha un timeout di 500ms in un worker thread
- `MCReader.writeBlock()` chiude la connessione nel `finally` di `valueTransferRestore()` ma NON in `writeBlock()` — la connessione resta aperta per scritture multiple
- Le chiavi sono `byte[6]` (12 char hex) — `Common.hex2Bytes()` converte da stringa

---

## PARTE 9 — RISORSE UI LEGACY

### Colori (`colors.xml` — invariato)

```
light_green #66FF66    dark_green  #338800
orange      #FF6600    purple      #9933FF
yellow      #CCCC00    red         #FF0000
light_gray  #DDDDDD    middle_gray #444444
dark_gray   #222222    white       #FFFFFF
```

### Localizzazioni

`en` (default), `es`, `fr`, `it`, `pt`, `ru`, `zh`, `zh-TW`

### Dialog Layout

| File | Scopo |
|------|-------|
| `dialog_donate.xml` | Finestra donazione |
| `dialog_save_file.xml` | Salvataggio file con nome |
| `dialog_write_sectors.xml` | Selezione settori da scrivere |
| `list_item_diff_block.xml` | Item lista DiffTool |
| `list_item_small_text.xml` | Item lista generica |
