package ru.yandex.music.autopiano.data;

import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author Marat Khairutdinov (wtiger)
 */
@Value
@EqualsAndHashCode
public class Melody {
    int id;
    @EqualsAndHashCode.Exclude
    String title;
}
