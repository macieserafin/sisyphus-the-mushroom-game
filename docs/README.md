# Sisyphus the Mushroom - dokument projektu

Ten dokument jest glownym zrodlem prawdy dla projektu. Zastepuje stare,
rozproszone notatki, powtarzajace sie plany i materialy robocze dla AI.

## 1. Wizja gry

`Sisyphus the Mushroom` to top-down 2D action roguelike w Java Swing.

Gracz steruje bohaterem uwiezionym w dziwnym grzybowym swiecie. Wychodzi z hubu
do runow bojowych, walczy z przeciwnikami, zbiera nagrody, buduje tymczasowy
build, przegrywa albo pokonuje bossow i wraca do hubu z nowymi celami.

Gra jest inspirowana struktura `The Binding of Isaac`, ale jej tozsamosc ma byc
wlasna:

- grzyby, zarodniki, grzybnia i mutacje,
- miejski, brudny prolog kontrastujacy z surrealnym swiatem,
- cykl smierci i przebudzen,
- walka laczaca melee i ranged,
- runy oparte na synergiach, ryzyku i dziwnych nagrodach,
- glowny antagonista BYT komentujacy porazki i postepy gracza.

Docelowe odczucie: gra ma byc czytelna, szybka, lekko niepokojaca, czasem
czarno-humorystyczna i nastawiona na "jeszcze jeden run".

## 2. Aktualny stan projektu

Projekt ma juz podstawowy vertical slice:

- hub/startowa lokacja,
- portal rozpoczynajacy run,
- recznie przygotowana mapa runu,
- przejscie z runu do areny bossa,
- podstawowa walka melee i ranged,
- przeciwnikow na mapie,
- testowego bossa `Slime King`,
- pasek HP bossa,
- portal konczacy run po pokonaniu bossa,
- powrot do hubu po zakonczeniu runu,
- poczatki prologu, dialogow i BYTA jako postaci gameplayowej.

Mapy sa nadal reczne. Proceduralna generacja jest planowana pozniej, gdy gra
bedzie miala wiecej przeciwnikow, nagrod, encounterow, bossow i event roomow.

## 3. Glowne cele projektu

Najwazniejsze cele gameplayowe:

1. Domknac grywalny loop: hub -> run -> boss -> nagroda/powrot.
2. Dodac nagrody, ktore zmieniaja sposob gry, nie tylko liczby.
3. Zrobic z melee + ranged najwazniejszy wyrznik walki.
4. Rozwinac prolog i BYTA tak, zeby smierc miala sens fabularny.
5. Dodac permanentna progresje w hubie.
6. Dopiero potem dodac generator poziomow.

Najwazniejsze cele techniczne:

1. Rozwijac projekt malymi krokami, bez przepisywania silnika.
2. Zostawic Java Swing i fixed update loop.
3. Dodawac abstrakcje dopiero wtedy, gdy pojawia sie drugi lub trzeci prawdziwy
   przypadek uzycia.
4. Nie robic ECS, duzego frameworka ani pelnego systemu skryptowego.
5. Utrzymac `LevelManager` jako wlasciciela przejsc miedzy lokacjami.

## 4. Najblizszy plan

Najpierw trzeba rozwijac to, co od razu poprawia grywalnosc.

### Etap 1 - reward loop

- Dodac walute: grzyby.
- Dodac drop waluty z przeciwnikow.
- Dodac licznik waluty zebranej w runie i waluty zapisanej w hubie.
- Dodac prosty power-up.
- Dodac chest dajacy power-up albo walute.
- Resetowac efekty tymczasowe po zakonczeniu runu.

### Etap 2 - pierwszy prawdziwy build

- Dodac jeden run item dla ranged.
- Dodac jeden run item dla melee.
- Dodac jeden item laczacy melee i ranged.
- Dodac krotki opis itemu po podniesieniu.
- Dodac feedback, gdy powstaje synergia.

Przykladowy zestaw:

- `Piercing Spores` - pociski przebijaja jednego przeciwnika.
- `Heavy Cap` - melee ma wiekszy knockback.
- `Marked Flesh` - ranged oznacza przeciwnika, melee detonuje znacznik.

### Etap 3 - lepszy prolog i BYT

- Uporzadkowac poczatek gry: VN prolog -> tutorial -> walka z BYTEM -> pierwsza
  smierc -> story lobby.
- Zostawic walke z BYTEM jako mechanicznie prawdziwa, ale fabularnie niemozliwa
  do wygrania.
- Po zbiciu okolo 10% HP BYT przerywa walke i zabija gracza skryptowo.
- BYT powinien pozniej komentowac runy, smierci, itemy i postepy gracza.

### Etap 4 - boss i encounter polish

- Rozbudowac `Slime King` o czytelniejsze patterny.
- Dodac telegraphy atakow.
- Dodac lepszy feedback trafien, smierci i wejscia w portal.
- Dodac pierwszego minibossa albo mocniejszy encounter.

### Etap 5 - hub i permanentna progresja

- Dodac prosty sklep albo NPC od zakupow.
- Dodac wybor podstawowego loadoutu.
- Dodac jedna permanentna bron lub umiejetnosc.
- Dodac jeden core item kupowany w hubie.
- Pozniej dodac zapis permanentnych odblokowan.

### Etap 6 - procedural generation

Generator powinien wejsc dopiero wtedy, gdy istnieja elementy, ktore warto
mieszac:

- kilka typow przeciwnikow,
- kilka run itemow,
- chesty,
- encountery,
- event roomy,
- miniboss,
- boss,
- nagrody i podstawowe ryzyko.

Pierwsza wersja generatora moze byc prosta: mapa lasu, gwarantowana sciezka,
polany, drzewa jako przeszkody, spawn gracza, portal do bossa i kilka
encounterow.

## 5. Fabula

Bohater zaczyna jako zwykly czlowiek z miejskiego srodowiska. Siedzi ze swoim
znajomym, pali zielsko i traktuje wieczor jak kolejna glupia wspolna akcje.
Znajomy prowadzi go do piwnicy, gdzie pokazuje mu dziwne, wilgotne,
lekko swiecace grzyby.

Obaj jedza kawalek. Rzeczywistosc zaczyna sie rozwarstwiac: dzwieki miasta
cichna, piwnica wydluza sie jak tunel, sciany porasta grzybnia, a bohater traci
przytomnosc.

Po przebudzeniu bohater trafia do grzybowej krainy. Nie wie, czy umarl, sni, ma
najgorszy trip zycia, czy naprawde zostal przeniesiony do innego swiata.
Ta niepewnosc ma zostac wazna przez cala gre.

Pierwsza istota, ktora sie do niego odzywa, to BYT. BYT nie pomaga i nie tlumaczy
swiata z troski. Obserwuje, testuje, szydzi i traktuje bohatera jak kolejna
ofiare w cyklu smierci.

Prolog konczy sie pierwsza walka z BYTEM. Gracz moze go trafic, ale nie moze
wygrac. BYT przerywa walke, zabija bohatera i w ten sposob gra pokazuje glowna
zasade roguelike'a: smierc nie konczy historii, tylko cofa bohatera do hubu.

Glowne pytania fabularne:

- Czy grzybowy swiat jest realny, czy jest halucynacja?
- Co stalo sie z cialem bohatera w piwnicy?
- Czy znajomy wiedzial, czym sa grzyby?
- Czy wioska istnieje niezaleznie od bohatera?
- Czy BYT wciaga ludzi do swojego swiata, czy tylko pasozytuje na tych, ktorzy
  sami otworzyli przejscie?
- Czy powrot do domu jest jeszcze mozliwy?

Gra nie musi od razu odpowiadac na te pytania. Powinny napedzac dialogi, event
roomy, komentarze BYTA i zakonczenia.

## 6. Postacie

### Bohater / Gracz

Na poczatku jest czlowiekiem z miasta. Jest zmeczony, ostrozny, sarkastyczny i
probuje udawac luz, nawet gdy sytuacja robi sie coraz gorsza.

Nie jest od razu wybrancem ani herosem. Jego sila ma wynikac z uporu, adaptacji
i pamieci kolejnych smierci.

W gameplayu staje sie malym grzybowym bohaterem. Ta przemiana nie powinna byc
slodka ani maskotkowa. Ma byc dziwna, organiczna i troche niepokojaca.

### Znajomy / Kolega

Bliski ziomek bohatera z prologu. Jest glosniejszy, bardziej pewny siebie,
zabawny i lekkomyslny. Pcha bohatera w dziwna sytuacje, ale na poczatku powinien
brzmiec jak prawdziwy kumpel, nie jak oczywisty zdrajca.

Z czasem mozna sugerowac, ze wie wiecej, niz mowi, albo ze reaguje zbyt spokojnie
na rzeczy, ktore powinny go przerazic.

### BYT

Glowny antagonista, komentator i przyszly finalny boss.

BYT jest inteligentny, pogardliwy, ironiczny i spokojny. Nie tlumaczy zasad
swiata z empatii. Jezeli instruuje gracza, robi to jak ktos, kto testuje slaba
ofiare.

Na poczatku jego ton jest rozbawiony i okrutny. Im dalej gracz zachodzi, tym
bardziej BYT powinien przechodzic w irytacje, niepokoj i gniew.

BYT powinien komentowac:

- smierci gracza,
- podniesione itemy,
- mutacje,
- pokonanych bossow,
- ryzykowne decyzje,
- momenty, w ktorych gracz zaczyna byc realnym zagrozeniem.

### NPC w wiosce

Wioska/hub jest miejscem powrotu po smierci. NPC moga pomagac, sprzedawac,
tlumaczyc czesc zasad i reagowac na postep, ale nie powinni od razu wyjasniac
wszystkiego.

Moga byc ciepli, dziwni, grzybowi, praktyczni albo niepokojacy. Hub powinien
dawac poczucie bezpieczenstwa, ale nie normalnosci.

## 7. Gameplay

### Glowna petla

Aktualna petla:

1. Start w hubie.
2. Wejscie do runu przez portal.
3. Eksploracja recznej mapy runu.
4. Walka z przeciwnikami.
5. Wejscie do areny bossa.
6. Pokonanie bossa.
7. Portal wyjsciowy.
8. Powrot do hubu.

Docelowa petla:

1. Przygotowanie w hubie.
2. Wybor broni, umiejetnosci i core itemow.
3. Start runu.
4. Eksploracja generowanych albo pol-generowanych poziomow.
5. Walka z przeciwnikami, minibossami i encounterami.
6. Zdobywanie tymczasowych itemow, power-upow i mutacji.
7. Budowanie synergii.
8. Walka z bossem pietra.
9. Decyzja: zejsc glebiej albo zakonczyc run.
10. Powrot do hubu z waluta i permanentnym postepem.

### Walka

Walka ma laczyc:

- ruch,
- unikanie,
- melee,
- ranged,
- statusy,
- knockback,
- synergie miedzy broniami,
- czytelne zachowania przeciwnikow.

Najwiekszym wyroznikiem gry powinno byc dynamiczne laczenie melee i ranged.
To nie maja byc dwa osobne style, tylko dwa narzedzia, ktore wzajemnie sie
napedzaja.

Przyklady synergii:

- Ranged oznacza przeciwnika, melee detonuje znacznik.
- Melee naklada zarodniki, ranged detonuje je w chmure trucizny.
- Trafienia melee przeladowuja czesc magazynka ranged.
- Odbity mieczem pocisk dostaje bonus ranged.
- Dash zostawia zarodniki, a strzal podpala albo zatruwa chmure.

Gra nie powinna calkowicie karac buildow skupionych tylko na melee albo tylko na
ranged, ale najlepsze efekty powinny powstawac z laczenia obu.

### Encountery

Encounter nie powinien oznaczac tylko grupy przeciwnikow stojacych w pokoju.

Docelowe typy:

- fale przeciwnikow,
- przetrwanie przez okreslony czas,
- obrona obiektu,
- walka w kurczacej sie arenie,
- przeciwnicy wychodzacy z gniazd,
- walka przy ograniczonej widocznosci,
- arena z toksyczna mgla albo korzeniami,
- opcjonalne wyzwania z dodatkowa nagroda.

### Bossowie

Boss ma testowac to, czego gracz uczy sie na pietrze.

`Slime King` jest obecnym testowym bossem. Docelowo mozna go rozwinac przez:

- skoki z fala uderzeniowa,
- rozdzielanie sie na mniejsze slime'y,
- faze enraged,
- przywolywanie leczacych slime'ow,
- czasowe odbijanie pociskow,
- momenty podatnosci na melee,
- toksyczna maz ograniczajaca arene.

Pokonanie bossa powinno miec konsekwencje, np. nowy NPC w hubie, nowy typ itemow,
nowa pula przeciwnikow albo zmiana wygladu hubu.

## 8. Progresja

Gra ma dwa oddzielne typy progresji.

### Progresja permanentna

Dzieje sie w hubie i zostaje miedzy runami.

Przyklady:

- nowe bronie,
- core itemy,
- umiejetnosci,
- wybor startowego loadoutu,
- ulepszenia hubu,
- nowe NPC,
- odblokowane pule run itemow.

Core itemy sa specjalne i stale. Nie powinny dropic w trakcie runu.

### Progresja tymczasowa

Dzieje sie w runie i resetuje po jego zakonczeniu.

Przyklady:

- power-upy,
- run itemy,
- mutacje,
- efekty pociskow,
- efekty melee,
- tymczasowe statusy,
- ryzykowne bonusy z chestow albo event roomow.

Power-up to prosty bonus statystyk, np. `HP up`, `speed up`, `damage up`.

Run item powinien zmieniac sposob gry. Jezeli przedmiot daje tylko `+10% damage`,
raczej jest power-upem, nie run itemem.

### Waluta

Podstawowa waluta to grzyby.

Zrodla:

- zwykli przeciwnicy,
- mocniejsi przeciwnicy,
- minibossowie,
- bossowie,
- chesty,
- event roomy,
- sekrety.

Do decyzji zostaje, ile waluty gracz zachowuje po smierci. Proponowany kierunek:
gracz zachowuje mala czesc po porazce, ale pelna wartosc dostaje po pokonaniu
bossa albo bezpiecznym zakonczeniu runu.

## 9. Chesty, event roomy i ryzyko

Chesty powinny byc czyms wiecej niz automatem z nagroda.

Podstawowy chest moze dawac:

- walute,
- power-up,
- prosty run item.

Pozniejsze warianty:

- chest z wyborem jednej z trzech nagrod,
- cursed chest z silnym itemem i wada,
- sacrifice chest za HP albo aktualny item,
- gambling chest za walute,
- mimic,
- delayed chest, ktory mozna otworzyc pozniej za lepsza nagrode.

Event roomy maja przerywac rytm samej walki i dawac decyzje:

- tajemniczy grzyb proszacy o nakarmienie,
- zatruta sadzawka dajaca mutacje za HP,
- stary mykolog ulepszajacy albo niszczacy item,
- martwy podroznik z notatka i ryzykiem walki,
- grzybowa kapliczka z wyborem nagrody i klatwy.

## 10. Mutacje i Overgrowth

Mutacje sa run itemami albo efektami, ktore widocznie zmieniaja bohatera.

Przyklady:

- kolce,
- swiecace oczy,
- dodatkowy grzyb na ciele,
- aura zarodnikow,
- toksyczny slad,
- grzybnia na skore.

Na start wystarcza proste overlaye sprite'ow. Nie trzeba robic osobnej pelnej
animacji dla kazdej kombinacji.

`Overgrowth` to opcjonalny system ryzyka: im bardziej gracz zarasta grzybnia,
tym wieksze bonusy dostaje, ale run staje sie bardziej niebezpieczny.

Przykladowe poziomy:

1. Normal.
2. Spores.
3. Overgrown.
4. Corrupted.
5. Ancient Fungus.

Glowne pytanie dla gracza: czy warto stac sie silniejszym, jezeli run przez to
robi sie bardziej niebezpieczny?

## 11. Narracja i dialogi

Dialogi prologu z bohaterem i znajomym powinny byc po angielsku, naturalne,
potoczne i krotkie. Inspiracja relacji: dwaj bliscy kumple, docinki, szybkie
riposty, luz przykrywajacy narastajacy strach.

Nie nalezy robic karykatury akcentu ani opierac humoru na stereotypach.

BYT powinien mowic mniej, ale celniej. Nie powinien dawac dlugich monologow
ekspozycyjnych. Jego kwestie maja brzmiec jak testowanie, szydzenie i stopniowo
rosnaca frustracja.

System dialogow powinien docelowo obslugiwac:

- cutscenki VN,
- rozmowy z NPC,
- dolny overlay dialogowy,
- ambient komentarze BYTA w czasie gameplayu,
- krotkie teksty po smierci, itemach i bossach.

Komentarze ambient nie powinny zatrzymywac walki. Wazne dialogi fabularne moga
blokowac gre.

## 12. Assety

Gra uzywa pixel artu.

Podstawowe proporcje:

- ekran gry: `768x576`, proporcje 4:3,
- gameplay sprite: zwykle `16x16`, skalowane x3,
- tile: `16x16`, wyswietlane jako `48x48`,
- VN background: `256x192`, wyswietlany x3,
- VN splash art: `128x192`, wyswietlany okolo x2,
- bottom overlay portrait: `50x50`.

Najwazniejsze brakujace assety:

1. `res/objects/portal.png` - portal zamiast grafiki drzwi.
2. VN backgroundy prologu:
   - `res/backgrounds/vn_street_evening.png`,
   - `res/backgrounds/vn_friend_house.png`,
   - `res/backgrounds/vn_friend_room.png`,
   - `res/backgrounds/vn_basement_stairs.png`,
   - `res/backgrounds/vn_basement_normal.png`,
   - `res/backgrounds/vn_basement_mushrooms.png`,
   - `res/backgrounds/vn_mushroom_closeup.png`,
   - `res/backgrounds/vn_fungal_tunnel.png`,
   - `res/backgrounds/vn_mushroom_forest.png`.
3. VN splash arty:
   - `res/dialogue/player_neutral.png`,
   - `res/dialogue/player_confused.png`,
   - `res/dialogue/player_scared.png`,
   - `res/dialogue/player_mushroom_world.png`,
   - `res/dialogue/friend_neutral.png`,
   - `res/dialogue/friend_excited.png`,
   - `res/dialogue/friend_nervous.png`,
   - `res/dialogue/friend_distorted.png`,
   - `res/dialogue/byt_shadow.png`,
   - `res/dialogue/byt_mocking.png`,
   - `res/dialogue/byt_angry.png`,
   - `res/dialogue/byt_trueform.png`.
4. Boss sprites:
   - `res/enemy/byt_idle_1.png`,
   - `res/enemy/byt_idle_2.png`,
   - `res/enemy/slime_king_idle_1.png`,
   - `res/enemy/slime_king_idle_2.png`,
   - `res/enemy/slime_king_enraged_1.png`,
   - `res/enemy/slime_king_enraged_2.png`.
5. Audio:
   - `res/sounds/cutscene.wav`,
   - `res/sounds/vn_city_loop.wav`,
   - `res/sounds/vn_fungal_loop.wav`,
   - `res/sounds/hub_loop.wav`,
   - `res/sounds/run_forest_loop.wav`,
   - `res/sounds/boss_slime_king_loop.wav`,
   - `res/sounds/portal_enter.wav`,
   - `res/sounds/portal_appear.wav`.

Najlepsza kolejnosc assetow:

1. Portal.
2. Muzyka cutscene albo VN.
3. Backgroundy VN prologu.
4. Minimalne splashe: player neutral/scared, friend neutral/nervous, BYT shadow.
5. Sprite'y BYTA i Slime Kinga.
6. SFX portalu, bossow i broni.
7. Polish tilesetu, broni i pociskow.

## 13. Zasady techniczne

Zostawiamy prosty kierunek projektu:

- bez duzego engine rewrite,
- bez ECS,
- bez dependency injection,
- bez event busa na zapas,
- bez generatora przed realna zawartoscia,
- bez wielkiego systemu itemow przed pierwszymi konkretnymi itemami.

Wazne istniejace elementy:

- `GamePanel` trzyma glowna petle i glowne systemy.
- `LevelManager` zarzadza lokacjami i przejsciami.
- `Portal` jest uniwersalnym obiektem przejsc.
- `Enemy` jest baza dla przeciwnikow i bossow.
- `SuperObject.onPlayerTouch()` jest glownym hookiem interakcji obiektow.

Najblizsze bezpieczne poprawki techniczne:

1. Naprawic obsluge brakujacych dzwiekow w `Sound`, zeby brak pliku nie konczyl
   sie ukrytym bledem albo NPE.
2. Dodac przelacznik debug draw dla hitboxow i debug UI.
3. Dodac minimalny model trafienia z typem `MELEE` / `RANGED`, gdy pojawi sie
   pierwszy enemy albo boss reagujacy inaczej na typ ataku.
4. Przejsc z tablic enemy/objects/NPC na listy przed proceduralna generacja,
   summonami i wiekszymi encounterami.
5. Odchudzac `Player` tylko wtedy, gdy konkretny nowy system tego wymaga.

## 14. Czego unikac

- Przepisywania calej gry.
- Generatora proceduralnego przed zawartoscia.
- Samych procentowych upgrade'ow.
- Wielu walut bez potrzeby.
- Bossow bedacych tylko wiekszym zwyklym przeciwnikiem.
- Loot table z dziesiatkami prawie identycznych przedmiotow.
- Oddzielania narracji od gameplayu.
- Dlugich ekspozycyjnych dialogow.
- Traktowania hubu jak zwyklego menu zakupow.

## 15. Minimalny vertical slice dajacy frajde

Minimalna wersja, ktora powinna pokazac potencjal gry:

- hub,
- prolog z BYTEM,
- jedna bron melee,
- jedna bron ranged,
- dash albo roll,
- trzy typy zwyklych przeciwnikow,
- jeden miniboss,
- jeden boss,
- trzy power-upy,
- piec run itemow,
- dwie synergie melee/ranged,
- dwa rodzaje chestow,
- jeden event room,
- waluta grzyby,
- powrot do hubu,
- jeden permanentny zakup.

Najwazniejsza zasada: gracz nie powinien wracac tylko po wieksze statystyki.
Powinien wracac, bo chce sprawdzic, co stanie sie po polaczeniu nowej mutacji,
broni i ryzykownej nagrody.
