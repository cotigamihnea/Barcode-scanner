# Implementare Decodor Coduri de Bare EAN-13

## Funcționalități

1. **Procesarea Avansată a Imaginilor**:
   - Citește fișiere brute în format `.ppm` (P6), gestionând header-ele și datele binare.
   - Transformă imaginile din format RGB în tonuri de gri (Grayscale) și ulterior în alb-negru (Black & White).
   - Utilizează un algoritm de **Adaptive Thresholding** (prag adaptiv) pentru a binariza corect imaginea, chiar și în condiții de iluminare inegală sau umbre.

2. **Aritmetică de Precizie și Compresie**:
   - Implementează o structură matematică proprie, `RatioInt`, pentru a lucra cu fracții ireductibile, eliminând erorile de precizie specifice calculelor în virgulă mobilă.
   - Folosește compresia **Run-Length Encoding (RLE)** pentru a procesa eficient secvențele de pixeli.

3. **Decodarea Standardului EAN-13**:
   - Identifică și distinge între tipurile de codificare a parității: L-code (Left Odd), G-code (Left Even) și R-code (Right).
   - Determină cifrele prin calculul "distanței" minime dintre barele scanate și modelele ideale din standard.
   - Reconstituie prima cifră a codului (care nu este reprezentată grafic) pe baza secvenței de parități a primelor 6 cifre.

4. **Validare, Integritate și Flux de Execuție**:
   - Verifică validitatea fiecărei linii scanate prin identificarea markerilor de start, mijloc și final.
   - Validează codul complet folosind algoritmul de checksum (cifra de control).
   - Scanează inteligent imaginea, analizând felii orizontale până la identificarea unui cod valid.

---

### 1. **Procesarea Imaginilor (Parser & Convertor)**
- **`parsePPM`**: Această funcție gestionează intrarea brută. Citește header-ul formatului P6 pentru a extrage dimensiunile, apoi procesează fluxul de octeți, grupându-i câte trei (R, G, B) pentru a construi matricea de pixeli `PPMImage`. Este primul pas crucial pentru a transforma datele binare într-o structură utilizabilă.

- **`toGreyScale`**: Convertește imaginea color într-una monocromă (`PGMImage`). Se aplică formula standard de luminanță ($0.3 \cdot R + 0.59 \cdot G + 0.11 \cdot B$), care reflectă percepția ochiului uman, pregătind imaginea pentru binarizare.

- **`toBlackAndWhite`**: Transformă imaginea în alb-negru pur (`PBMImage`) folosind o tehnică robustă de **Adaptive Thresholding**. În loc să folosească un singur prag pentru toată imaginea, algoritmul împarte imaginea în 4 cadrane și calculează un pivot local (media dintre minim și maxim) pentru fiecare. Astfel, decodarea funcționează chiar dacă o parte a etichetei este mai întunecată decât alta.

### 2. **Gestionarea Datelor și Aritmetica (Types)**
- **`RatioInt`**: O inovație cheie a proiectului este utilizarea fracțiilor exacte în locul numerelor reale. Deoarece un cod de bare poate fi scanat de la distanțe diferite, dimensiunea în pixeli variază. `RatioInt` stochează proporțiile sub formă de numărător/numitor (simplificate prin CMMDC), permițând compararea exactă a lățimii barelor indiferent de rezoluția imaginii.

- **`runLength` și `scaleToOne`**: Funcția `runLength` comprimă șirul de biți (ex: `0001100`) în perechi de tipul `(lungime, valoare)`. Ulterior, `scaleToOne` normalizează aceste lungimi, transformându-le în fracții `RatioInt` relative la lățimea totală a grupului curent. Acest pas asigură invarianța la scalare.

### 3. **Logica de Decodare (Decoder)**
- **`distance` și `bestMatch`**: Pentru a identifica o cifră, algoritmul calculează "distanța" matematică dintre proporțiile barelor scanate și modelele ideale (hardcodate în dicționare). Funcția `bestMatch` alege cifra care minimizează această eroare, asigurând cea mai probabilă potrivire chiar și în cazul unor mici imperfecțiuni de printare.

- **`findLast12Digits`**: Această funcție sparge fluxul continuu de date RLE în grupuri de câte 4 segmente (bară-spațiu-bară-spațiu). Pentru fiecare grup, determină paritatea (Odd/Even) și cifra asociată, reconstruind astfel cele 12 cifre vizibile ale codului de bare.

- **`firstDigit`**: Standardul EAN-13 ascunde prima cifră în modelul de paritate al părții stângi a codului. Funcția analizează secvența de parități (de exemplu, `LGGLGG`) și o compară cu tabelul standard pentru a deduce prefixul codului, completând astfel setul de 13 cifre.

### 4. **Fluxul Principal și Validare (Main)**
- **`checkRow`**: Înainte de a încerca decodarea, această funcție filtrează "zgomotul". Verifică dacă linia de pixeli respectă structura anatomică a unui cod de bare: markerii de gardă (101) la capete și în mijloc, și lungimea fixă de 59 de unități RLE.

- **`checkDigit`**: Implementează verificarea matematică finală (Algoritmul Modulo 10). Se calculează o sumă ponderată a primelor 12 cifre (pozițiile impare înmulțite cu 1, cele pare cu 3). Rezultatul trebuie să valideze a 13-a cifră; în caz contrar, citirea este considerată eronată.

- **`readBarcodes`**: Funcția care orchestrează întregul sistem. Parcurge fișierele din directorul de intrare, aplică transformările de imagine, decupează o porțiune centrală și încearcă decodarea linie cu linie. Procesul se oprește la prima linie validă găsită, garantând eficiența.