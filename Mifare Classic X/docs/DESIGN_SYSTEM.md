# MiFareX — direzione UI P3

## Identita' del prodotto

MiFareX e' una console tecnica offline per operazioni NFC. L'interfaccia deve far
percepire precisione, controllo e sicurezza, distinguendo senza ambiguita' il lavoro
quotidiano dell'operatore dalla configurazione Admin e dagli strumenti raw Expert.

## Direzioni valutate

### 1. Industrial Premium — scelta

- Light: primary `#00677D`, background `#F5FAFC`, surface `#E9F0F3`, warning
  `#855300`, error `#BA1A1A`.
- Dark: primary `#5DD8F3`, background `#071116`, surface `#102027`, warning
  `#FFB95F`, error `#FFB4AB`.
- Tipografia di sistema, titoli semibold/bold, label tecniche con spaziatura maggiore.
- Card da 20 dp, campi e dialog da 14-20 dp, pulsanti primari da almeno 48 dp.
- Icone Material piene per stati e azioni primarie; outlined solo per azioni secondarie.
- Elevazione contenuta (0-2 dp, 4-5 dp durante la pressione), bordi sottili e accenti
  cromatici legati alla categoria.
- Gradienti solo su hero e icone; nessun effetto decorativo nelle aree di errore.
- Microinterazioni brevi, stepper esplicito e feedback aptico differenziato.
- Vantaggi: identita' riconoscibile, ottima modalita' scura, semantica di sicurezza.
- Svantaggi: meno neutra di una UI enterprise e richiede palette controllata; per
  questo il dynamic color Android e' disabilitato.

### 2. Dark Tech Professional

- Background `#05070B`, primary `#00E5FF`, secondary `#7C4DFF`, success `#35E28B`,
  warning `#FFC857`, error `#FF5C69`.
- Tipografia piu' monospaziata, bordi luminosi, card da 12 dp e gradienti radiali.
- Vantaggi: forte carattere tecnico e ottima riconoscibilita' Expert.
- Svantaggi: look aggressivo, leggibilita' light debole e rischio di sembrare uno
  strumento sperimentale anziche' professionale.

### 3. Clean Enterprise

- Background `#F7F9FC`, primary `#2457A7`, secondary `#526579`, success `#287A4B`,
  warning `#946200`, error `#B3261E`; dark basato su `#111820`.
- Card quasi piatte da 12 dp, nessun gradiente, icone outlined e densita' alta.
- Vantaggi: familiarita', ottimo uso su tablet e manutenzione semplice.
- Svantaggi: comunica meno il carattere NFC e separa con minore forza Operatore ed
  Expert.

## Specifica applicata

- `MctxTheme` controlla palette, tipografia e shape in light/dark.
- Operatore usa ciano e CTA singole; Admin usa badge dedicati; Expert usa ambra ed e'
  raggiungibile soltanto dal Control Center.
- Lo stato NFC e' sempre testuale oltre che cromatico.
- La scrittura segue `Verifica -> Scrittura -> Conferma`, con riepilogo prima di
  armare la sessione e indicazione esplicita del read-back.
- Home e Control Center usano griglie adattive; i form e i dettagli sono limitati a
  840-920 dp per non disperdersi sui tablet.
- Onboarding e lock passano a layout affiancato da 700/720 dp; su telefono restano
  verticali e raggiungibili con una mano.
- Target interattivi: almeno 48 dp. Icone decorative non espongono descrizioni;
  azioni e stato NFC hanno descrizioni specifiche per TalkBack.
- Loading, empty, success ed error usano componenti distinti e testo esplicito. Il
  colore non e' mai l'unico segnale.

## Copertura delle schermate

- Onboarding: introduce profili, sicurezza del flusso e separazione dei ruoli.
- Lock: spiega quali dati protegge la biometria.
- Home/vendor grid: dashboard operatore, stato NFC e profili adattivi.
- Vendor card/detail: stato verificato, gerarchia azioni e conferma pre-scrittura.
- Scrittura/Auto Mode/risultati: sessione armata visibile, stepper, read-back e retry.
- Vendor editor/import-export/UID: contenitori Admin, larghezza massima e messaggi di
  sicurezza.
- Expert e impostazioni: accesso dal Control Center alle Activity legacy, mantenute
  separate per evitare operazioni raw accidentali.

Le Activity Expert legacy conservano per ora i layout esistenti: la loro migrazione
completa a Compose e' un lavoro autonomo ad alto rischio, perche' contiene tutti gli
strumenti low-level MIFARE. P3 ne corregge discovery, separazione e contesto visuale
senza modificare il comportamento NFC sottostante.
