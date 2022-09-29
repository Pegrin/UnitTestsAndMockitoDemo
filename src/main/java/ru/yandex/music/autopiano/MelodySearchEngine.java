package ru.yandex.music.autopiano;

import java.util.List;
import java.util.UUID;

import ru.yandex.music.autopiano.data.Melody;

/**
 * @author Marat Khairutdinov (wtiger)
 */
public interface MelodySearchEngine {
    Melody findMelody(String melodySearchPhrase, List<Integer> c);
}
