![[logo_wydzial.png]]

| **Imię i Nazwisko**     | **Nr Albumu** | **Przedmiot**                       | **Semestr** |
| ----------------------- | ------------- | ----------------------------------- | ----------- |
| **Damian Skonieczny**   | 122421        | Programowanie Współbieżne           | V           |
| **Prowadzący**          | **Typ zajęć** | **Temat**                           |             |
| **dr Damian Ledziński** | Laboratorium  | Analiza porównawcza mechanizmów wielowątkowości w zadaniach typu I/O-bound |             |

# 1. Opis tematu i wprowadzenie

Celem serii laboratoriów było zaimplementowanie oraz optymalizacja algorytmu przeszukiwania grafu powiązań Wikipedii (Web Scraping) przy użyciu różnych mechanizmów współbieżności dostępnych na platformie JVM (Kotlin/Java). 

Problem polega na pobieraniu linków z artykułów Wikipedii na określoną głębokość (algorytm BFS). Zadanie to jest klasyfikowane jako **I/O-bound**, ponieważ procesor większość czasu spędza na oczekiwaniu na odpowiedź z serwerów sieciowych. W takim scenariuszu programowanie sekwencyjne jest skrajnie nieefektywne, a wybór odpowiedniego modelu współbieżności ma kluczowe znaczenie dla przepustowości systemu.

W ramach prac przeanalizowano 7 różnych podejść: od jednowątkowego, przez ręczne zarządzanie wątkami, aż po nowoczesne abstrakcje asynchroniczne i funkcyjne.

# 2. Charakterystyka mechanizmów

W każdym laboratorium zastosowano inny mechanizm zarządzania wątkami:

1.  **wikicount (Jednowątkowy):** Standardowe podejście sekwencyjne. Wykorzystuje kolejkę `ArrayDeque` do przetwarzania linków jeden po drugim.
2.  **Lab 2 (Wątki Manualne):** Wykorzystanie klasy `Thread`. Dla każdego artykułu na danym poziomie BFS tworzony jest nowy wątek. Synchronizacja poziomów odbywa się poprzez metodę `.join()`.
3.  **lab 3 (Ręczna Synchronizacja):** Podobnie jak w Lab 2, ale z naciskiem na bezpieczeństwo pamięci współdzielonej. Zastosowano bloki `synchronized` oraz monitory do ochrony niesynchronizowanych kolekcji wyników.
4.  **lab 4 (ExecutorService / Pula Wątków):** Wykorzystanie `Executors.newFixedThreadPool`. Rozwiązanie eliminuje kosztowny proces ciągłego tworzenia i niszczenia wątków, reużywając istniejącą pulę o rozmiarze dostosowanym do liczby rdzeni logicznych.
5.  **Lab 5 (Parallel Streams):** Podejście deklaratywne wykorzystujące `ForkJoinPool.commonPool()`. Abstrakcja pozwalająca na równoległe przetwarzanie kolekcji bez jawnego zarządzania wątkami.
6.  **Lab 6 (BlockingQueue - Producent/Konsument):** Implementacja wzorca projektowego z użyciem `LinkedBlockingQueue`. Wątki-konsumenci pobierają zadania z kolejki blokującej, co pozwala na płynne przetwarzanie bez sztywnego podziału na poziomy BFS.
7.  **Lab 7 (CompletableFuture):** Programowanie asynchroniczne oparte na obietnicach. Wykorzystuje łańcuchowanie zadań (`supplyAsync`, `thenAccept`) i nieblokujące operacje I/O.

# 3. Metodologia pomiarów

Podczas prac opracowano autorski system pomiarowy, mający na celu wyeliminowanie błędów wynikających z narzutu środowiska uruchomieniowego:

*   **Eliminacja narzutu JVM/Gradle:** Zamiast pomiarów zewnętrznych (np. `Measure-Command`), zaimplementowano precyzyjny pomiar czasu bezpośrednio w kodzie źródłowym (`System.currentTimeMillis()`) otaczający wyłącznie funkcję algorytmiczną.
*   **Robust Input Handling:** Aby uniknąć problemów z kodowaniem znaków i błędami 404 w systemie Windows (wynikającymi z interaktywnego `readln()`), programy zostały zrefaktoryzowane tak, aby przyjmowały parametry (hasło, głębokość) poprzez argumenty wiersza poleceń (`args`), przekazywane przez Gradle za pomocą flagi `-Pargs`.
*   **Weryfikacja pracy:** Każdy pomiar rejestruje nie tylko czas, ale i całkowitą liczbę unikalnych linków, co pozwala upewnić się, że każdy mechanizm wykonał porównywalną pracę.
*   **Parametry testu:** Hasło: "Warszawa", Głębokość: 3, Limit linków na stronę: 50.

# 4. Wyniki i Podsumowanie

Poniższa tabela przedstawia zbiorcze wyniki pomiarów wydajności:

| Nr_Laba | Mechanizm Wątków | Czas Wykonania (s) | Znalezione Linki |
| :--- | :--- | :--- | :--- |
| **wikicount** | Jednowątkowy | 162,73 s | 18 020 |
| **Lab 2** | Wątki (Manualne) | 42,78 s | 18 563 |
| **lab 3** | Wątki + Ręczna Synchronizacja | 35,71 s | 19 285 |
| **lab 4** | Pula Wątków (ExecutorService) | 15,77 s | 20 592 |
| **Lab 5** | Parallel Stream | 15,97 s | 21 151 |
| **Lab 6** | Kolejka Blokująca (BlockingQueue) | 16,94 s | 21 571 |
| **Lab 7** | CompletableFuture | 15,88 s | 23 269 |

# 5. Wnioski

1.  **Drastyczny wzrost wydajności:** Przejście z modelu jednowątkowego na współbieżny skróciło czas wykonania o około **90%** (z 162 s do ok. 16 s). Potwierdza to, że w zadaniach I/O-bound współbieżność jest niezbędna do efektywnego wykorzystania przepustowości łącza.
2.  **Wyższość puli wątków:** Mechanizmy wykorzystujące pule (Lab 4, 5, 7) okazały się znacznie szybsze od ręcznego tworzenia wątków (Lab 2). Narzut na systemowy systemowy proces tworzenia obiektu `Thread` jest mierzalny i negatywnie wpływa na czas przy dużej liczbie zadań.
3.  **Stabilność abstrakcji wysokopoziomowych:** `Parallel Streams` oraz `CompletableFuture` oferują nie tylko wysoką wydajność, ale również czytelniejszy kod, co redukuje ryzyko wystąpienia błędów typu *race condition* czy *deadlock*.
4.  **Wpływ niedeterminizmu:** Różnice w liczbie znalezionych linków wynikają z wyścigów w dostępie do globalnego zbioru `visited`. Kolejność, w jakiej wątki zgłaszają znalezienie linku, zmienia strukturę drzewa BFS, co przy narzuconym limicie 50 linków na stronę prowadzi do eksploracji nieco innych gałęzi grafu.
