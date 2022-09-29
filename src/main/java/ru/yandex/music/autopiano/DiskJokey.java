package ru.yandex.music.autopiano;

import ru.yandex.music.autopiano.data.Melody;

/**
 * @author Marat Khairutdinov (wtiger)
 */
public interface DiskJokey {
    boolean isMelodyValid(Melody melody);

    void playMelody(Melody melody);
}
