# Pomiary czasu wykonania laboratoriów - Przeszukiwanie Wikipedii

Hasło: **"Warszawa"**
Głębokość: **3**
Limit linków na stronę: **50**

Poniższa tabela przedstawia **czysty czas wykonania algorytmu** (z pominięciem startu JVM/Gradle), mierzony bezpośrednio w kodzie aplikacji.

| Nr_Laba | Mechanizm Wątków | Czas Wykonania (s) | Znalezione Linki |
| :--- | :--- | :--- | :--- |
| wikicount | Jednowątkowy | 162,73 s | 18 020 |
| Lab2 | Wątki (Manualne) | 42,78 s | 18 563 |
| lab3 | Wątki + Ręczna Synchronizacja | 35,71 s | 19 285 |
| lab4 | Pula Wątków (ExecutorService) | 15,77 s | 20 592 |
| Lab5 | Parallel Stream | 15,97 s | 21 151 |
| Lab6 | Kolejka Blokująca (BlockingQueue) | 16,94 s | 21 571 |
| Lab7 | CompletableFuture | 15,88 s | 23 269 |

### Wnioski:
1. **Przewaga współbieżności:** Wersje wielowątkowe (Lab4-Lab7) są ok. **10 razy szybsze** od wersji jednowątkowej (wikicount). Wynika to z faktu, że większość czasu program spędza na oczekiwaniu na odpowiedź z sieci (I/O), co w wersji jednowątkowej dzieje się sekwencyjnie.
2. **Efektywność zarządzania wątkami:** Pula wątków (**Lab4**) oraz Parallel Stream (**Lab5**) osiągnęły najlepsze rezultaty, minimalizując narzut na tworzenie nowych obiektów wątków (co widać w **Lab2**, gdzie dla każdego linku tworzony jest osobny `Thread`).
3. **Liczba linków:** Różnice w liczbie znalezionych linków wynikają z niedeterministycznej natury wielowątkowego przeszukiwania (różna kolejność odwiedzin przy globalnym zbiorze `visited`) oraz ewentualnych timeoutów przy dużym zagęszczeniu zapytań.

*Pomiary wykonane bezpośrednio wewnątrz aplikacji przy użyciu `System.currentTimeMillis()`.*