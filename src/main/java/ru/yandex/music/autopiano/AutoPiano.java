package ru.yandex.music.autopiano;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.music.autopiano.data.Melody;

/**
 * @author Marat Khairutdinov (wtiger)
 */
public class AutoPiano implements AutoCloseable {
    private final DiskJokey diskJokey;
    private final MelodySearchEngine melodySearchEngine;
    private final MiniDisplay miniDisplay;

    public AutoPiano(DiskJokey diskJokey, MelodySearchEngine melodySearchEngine, MiniDisplay miniDisplay) {
        this.diskJokey = diskJokey;
        this.melodySearchEngine = melodySearchEngine;
        this.miniDisplay = miniDisplay;
    }

    public void playMelody(String melodySearchPhrase) {
        List<Integer> failedMelodiesIds = new ArrayList<>();
        Melody melody = melodySearchEngine.findMelody(melodySearchPhrase, List.copyOf(failedMelodiesIds));
        while (!diskJokey.isMelodyValid(melody)) {
            failedMelodiesIds.add(melody.getId());
            melody = melodySearchEngine.findMelody(melodySearchPhrase, List.copyOf(failedMelodiesIds));
        }
        innerMagicProcessor();
        miniDisplay.announce(melody);
        diskJokey.playMelody(melody);
    }

    void innerMagicProcessor() {
        //Some magic staff we are doing here, comrade.
        throw new RuntimeException("Call to YT failed");
    }

    @Override
    public void close() throws Exception {
        if (miniDisplay instanceof AutoCloseable) {
            ((AutoCloseable) miniDisplay).close();
        }
    }
}
