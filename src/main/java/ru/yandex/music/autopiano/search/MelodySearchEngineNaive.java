package ru.yandex.music.autopiano.search;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import ru.yandex.music.autopiano.MelodySearchEngine;
import ru.yandex.music.autopiano.data.Melody;

/**
 * @author Marat Khairutdinov (wtiger)
 */
public class MelodySearchEngineNaive implements MelodySearchEngine {
    public static final List<Melody> MELODIES = List.of(
            new Melody(1, "Melody one"),
            new Melody(2, "Melody two"),
            new Melody(3, "Melody three"),
            new Melody(4, "Melody four"),
            new Melody(5, "Melody five")
    );

    @Override
    public Melody findMelody(String melodySearchPhrase, List<Integer> excludedMelodiesIds) {
        return MELODIES.stream()
                .filter(melody -> !excludedMelodiesIds.contains(melody.getId()))
                .findFirst()
                .orElseGet(() -> new Melody(ThreadLocalRandom.current().nextInt(100, 1000), "Random"));
    }
}
