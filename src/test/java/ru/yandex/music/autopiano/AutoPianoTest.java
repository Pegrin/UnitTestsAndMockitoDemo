package ru.yandex.music.autopiano;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import ru.yandex.music.autopiano.data.Melody;
import ru.yandex.music.autopiano.search.MelodySearchEngineNaive;

/**
 * @author Marat Khairutdinov (wtiger)
 */
//1. Объявляем юнит-тест в том же package, что и тестируемый класс: ru.yandex.music.autopiano
//2. Имя тест-класса совпадает с именем тестируемого класс, но добавляем
class AutoPianoTest {

    @Mock(answer = Answers.RETURNS_DEFAULTS) //Рассмотрим виды Answers подробнее
    private DiskJokey diskJokey;

    //У нас есть реализация, она годится для теста, мы не хотим усложнять тест, мокая ее. Используем Spy
    //Мокито может попытаться создать инстанс, это требует конструктор без аргументов и явное указание имплементации
    @Spy
    private MelodySearchEngine melodySearchEngine = new MelodySearchEngineNaive();

    @Mock(extraInterfaces = AutoCloseable.class)
    private MiniDisplay miniDisplay;

    //@InjectMocks - Не рекомендую использовать эту аннотацию (магия, протухающий код)
    private AutoPiano autoPiano;

    private AutoCloseable openedMocks;

    //Моки инициализируются в BeforeEach, а не в BeforeAll
    @BeforeEach
    void setUp() {
        openedMocks = MockitoAnnotations.openMocks(this);

        //Изначальный план:
        //autoPiano = new AutoPiano(diskJokey, melodySearchEngine, miniDisplay);

        //У нас есть метод "innerMagicProcessor", работу которого мы не можем проверить в тестовом окружении
        //потому что он всегда бросает исключение
        //Что делать? Замокаем его. Для этого наш тестируемый класс должен стать мокой.
        //Но какой толк от тестирования моки?
        //Делаем наш класс мокой с дефолтным поведением класса - Spy

        autoPiano = Mockito.spy(new AutoPiano(diskJokey, melodySearchEngine, miniDisplay));
        Mockito.doNothing().when(autoPiano).innerMagicProcessor();

        //Mockito.when().then()
        //Получится использовать только если:
        // 1. Метод мока не void
        // 2. Метод мока может быть вызван в текущий момент и это не приведет к исключению.
    }

    @AfterEach
    void tearDown() throws Exception { //Пробрасывать исключения из тестовых методов - это нормальная практика
        openedMocks.close();
    }


    //SpyAndMock
    interface ForMocking {
        int getInt();

        Long getLong();

        List<Integer> getList();

        Map<Long, String> getMap();

        Stream<Integer> getStream();

        String getString();

        Object getObject();
    }

    @Nested
    class SpyAndMockAnswersAndDefaults {
        @Test
        void mock_andDefaultAnswers() {
            ForMocking mock = Mockito.mock(ForMocking.class);

            //Primitives and boxed primitives are zeros
            Assertions.assertEquals(0, mock.getInt());
            Assertions.assertEquals(0L, mock.getLong());

            //Collections are empty collections
            Assertions.assertEquals(Collections.emptyList(), mock.getList());
            Assertions.assertEquals(Collections.emptyMap(), mock.getMap());
            Assertions.assertEquals(Collections.emptyList(), mock.getStream().collect(Collectors.toList()));

            //Other types are null by default
            Assertions.assertNull(mock.getString());
            Assertions.assertNull(mock.getObject());
        }

        @Test
        void mock_andNotSoDefaultAnswers() {
            Mockito.mock(ForMocking.class, Mockito.RETURNS_DEFAULTS);
            Mockito.mock(ForMocking.class, Mockito.CALLS_REAL_METHODS);
            Mockito.mock(ForMocking.class, Mockito.RETURNS_MOCKS);
            Mockito.mock(ForMocking.class, Mockito.RETURNS_DEEP_STUBS);
            Mockito.mock(ForMocking.class, Mockito.RETURNS_SMART_NULLS);
            Mockito.mock(ForMocking.class, Mockito.RETURNS_SELF);

            Mockito.mock(ForMocking.class, Mockito.withSettings());
        }

        @Test
        void stubbing_mock() {
            Mockito.when(diskJokey.isMelodyValid(Mockito.any())).thenReturn(true);
            //Mockito.when(diskJokey.isMelodyValid(Mockito.any())).thenCallRealMethod(); //Здесь не сработает
            Mockito.when(diskJokey.isMelodyValid(Mockito.any())).then(invocation -> {
                //Здесь может быть сложная логика
                int modBy2 = invocation.getArgument(0, Melody.class).getId() % 2;
                return modBy2 == 0;
            });
            //Или бросим исключение: Mockito.when(diskJokey.isMelodyValid(Mockito.any())).thenThrow(RuntimeException
            // .class);
        }

        @Test
        void spy_andDefaultBehavior() {
            List<String> realObject = List.of("One", "Two");
            List<String> spy = Mockito.spy(realObject);
            //Doesnt work: Mockito.when(spy.add("Three")).thenReturn(false); realObject is immutable
            Mockito.doReturn(false).when(spy).add("Three");

            Assertions.assertFalse(spy.add("Three"));
            Assertions.assertEquals(2, spy.size());
            Assertions.assertTrue(spy.contains("One"));
        }

        //Разберем подробнее еще раз
        @Test
        void stubbing_spy_realMethodMightBeCall() {
            //Важно помнить, что обращение к методу может быть выполнено.
            List<Integer> listSpy = Mockito.spy(new ArrayList<>());
            Mockito.when(listSpy.add(10)).thenReturn(false);

            Assertions.assertEquals(1, listSpy.size()); //Пока мы мокали Spy, мы мутировали его состояние
        }

        //Как же нужно в итоге?
        @Test
        void stubbing_spy_correctWay() {
            List<Integer> listSpy = Mockito.spy(new ArrayList<>());
            Mockito.doReturn(false).when(listSpy).add(10);
            //Или Mockito.doCallRealMethod().when(listSpy).add(10);

            Assertions.assertEquals(0, listSpy.size()); //Пока мы мокали Spy, мы мутировали его состояние
        }

        @Test
        void matchers() {
            Melody expectedMelody = new Melody(1, "test");
            Mockito.doReturn(expectedMelody)
                    .when(melodySearchEngine).findMelody(Mockito.startsWith("M1"), Mockito.eq(Collections.emptyList()));
            //В примере выше нельзя использовать findMelody(Mockito.startsWith("M1"), Collections.emptyList());
            //Если используется матчер в одном из аргументов,
            // то матчер должен быть использован для каждого из аргументов.

            Assertions.assertEquals(
                    expectedMelody,
                    melodySearchEngine.findMelody("M1Melody", Collections.emptyList())
            );
        }
    }

    @Nested
    class VerifyAndVerifyInOrder {
        @Test
        void playMelody_whenMelodyIsValid_thenMelodyIsPlayed() {
            givenEveryMelodyIsValid();

            autoPiano.playMelody("melody");

            Mockito.verify(diskJokey).playMelody(Mockito.any(Melody.class));
            //Варианты:
            // Mockito.verify(diskJokey, Mockito.times(2)).playMelody(Mockito.any(Melody.class));

            // Mockito.verify(diskJokey, Mockito.never()).playMelody(Mockito.any(Melody.class));
            //Mockito.never() == Mockito.times(0)

            // Mockito.verify(diskJokey, Mockito.only()).playMelody(Mockito.any(Melody.class));
            // Mockito.only() != Mockito.times(1)

            // Mockito.verify(diskJokey, Mockito.atLeastOnce()).playMelody(Mockito.any(Melody.class));
            // Mockito.verify(diskJokey, Mockito.atLeast(10)).playMelody(Mockito.any(Melody.class));

            // Mockito.verify(diskJokey, Mockito.atMostOnce()).playMelody(Mockito.any(Melody.class));
            // Mockito.verify(diskJokey, Mockito.atMost(2)).playMelody(Mockito.any(Melody.class));
        }

        //Пару слов об именовании методов
        @Test
        void playMelody_whenMelodyIsValid_thenBaseFlow() {
            givenEveryMelodyIsValid();

            String melodyRequest = "melody";
            autoPiano.playMelody(melodyRequest);

            //Порядок перечисления при инициализации - не важен
            InOrder callsInOrder = Mockito.inOrder(diskJokey, melodySearchEngine, miniDisplay, autoPiano);
            callsInOrder.verify(melodySearchEngine).findMelody(melodyRequest, Collections.emptyList());
            callsInOrder.verify(diskJokey).isMelodyValid(Mockito.any(Melody.class));
            callsInOrder.verify(autoPiano).innerMagicProcessor();
            callsInOrder.verify(miniDisplay).announce(Mockito.any(Melody.class));
            callsInOrder.verify(diskJokey).playMelody(Mockito.any(Melody.class));

            callsInOrder.verifyNoMoreInteractions(); //Не стоит злоупотреблять

            //Важно Mockito.any(Melody.class) != Mockito.any()
            //Mockito.any() - включает null и vararg
        }

        @Test
        void playMelody_whenMelodyIsValid_thenMelodyIsAnnounced_andThenMelodyIsPlayed() {
            givenEveryMelodyIsValid();

            autoPiano.playMelody("melody");

            //Порядок перечисления при инициализации - не важен
            InOrder callsInOrder = Mockito.inOrder(diskJokey, miniDisplay);
            callsInOrder.verify(miniDisplay).announce(Mockito.any(Melody.class));
            callsInOrder.verify(diskJokey).playMelody(Mockito.any(Melody.class));

            callsInOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    class ArgumentCaptorExamples {
        ArgumentCaptor<Melody> melodyVerifyArgumentCaptor = ArgumentCaptor.forClass(Melody.class);

        @Test
        void playMelody_whenMelody1IsInvalid_andMelody2IsValid_thenVerifyCalledTwoTimes() {
            Mockito.when(diskJokey.isMelodyValid(Mockito.any(Melody.class)))
                    .thenReturn(false, true);

            autoPiano.playMelody("melody");

            Mockito.verify(diskJokey, Mockito.times(2)).isMelodyValid(melodyVerifyArgumentCaptor.capture());

            List<Melody> melodiesForValidation = melodyVerifyArgumentCaptor.getAllValues();

            Assertions.assertEquals(2, melodiesForValidation.size());
            Assertions.assertEquals(MelodySearchEngineNaive.MELODIES.get(0), melodiesForValidation.get(0));
            Assertions.assertEquals(MelodySearchEngineNaive.MELODIES.get(1), melodiesForValidation.get(1));
        }

        //Когда бывает нужен?
        //Когда не получается использовать argument matcher в verify,
        // например, очень сложный или невоспроизводимый объект.
        //Когда у нас множество вызовов, и нужно найти среди них интересующий нас.
        //Когда нужно проверить какой-то паттерн для цепочки вызовов.
    }

    @Nested
    class TipsAndTricks {
        //Oneliner
        @Test
        void oneliner_demo() {
            List<String> listMock = Mockito.when(Mockito.mock(List.class).size()).thenReturn(100).getMock();


            Assertions.assertEquals(100, listMock.size());
        }


        //Mockito.reset in the middle of code - code smell
        @Test
        void name() {
            Mockito.reset(autoPiano, melodySearchEngine, miniDisplay);
            //Когда бывает нужен?
            //Для моков в контексте спринга (дешевле сбросить мок, чем переподнять весь контекст)
            //Почему стоит избегать?
            //Сбрасывает любой застабленное поведение, в том числе и предопределенное,
            // может усложнять диагностику и маскировать проблемы
        }


        //Стабинг финальных методов
        @Test
        void stubFinalMethod() {
            MyClassWithFinalMethod mockWithFinalMethod = Mockito.mock(
                    MyClassWithFinalMethod.class,
                    Mockito.withSettings().mockMaker(MockMakers.INLINE)
            );
            Mockito.when(mockWithFinalMethod.finalMethod()).thenReturn(1000L);

            Assertions.assertEquals(1000L, mockWithFinalMethod.finalMethod());
        }

        //Стабинг статических методов
        @Test
        void stubStaticMethod() {
            try (MockedStatic<MyClassWithStaticMethod> mockStatic = Mockito.mockStatic(MyClassWithStaticMethod.class)) {
                mockStatic.when(() -> MyClassWithStaticMethod.staticMethod()).thenReturn(1000L);
                Assertions.assertEquals(1000L, MyClassWithStaticMethod.staticMethod());
            }

            Assertions.assertEquals(0L, MyClassWithStaticMethod.staticMethod());
        }

        //Аналогично можно застабить поведение конструкторов класса через Mockito.mockConstruction
    }

    static class MyClassWithFinalMethod {
        public final Long finalMethod() {
            return 0L;
        }
    }

    static class MyClassWithStaticMethod {
        public static Long staticMethod() {
            return 0L;
        }
    }

    @Nested
    class BDDMockitoExamples {
        @Test
        void name() {
            BDDMockito.given(diskJokey.isMelodyValid(Mockito.any())).willReturn(false, true);

            String melodyRequest = "melody";
            autoPiano.playMelody(melodyRequest);

            BDDMockito.verify(melodySearchEngine).findMelody(melodyRequest, Collections.emptyList());
            BDDMockito.verify(melodySearchEngine).findMelody(melodyRequest, List.of(1));
        }
    }

    @Test
    void whenDisplayInAutoCloseable_andAutoPianoIsClosed_thenDisplayIsClosedToo() throws Exception {
        autoPiano.close();

        Mockito.verify((AutoCloseable) miniDisplay, Mockito.times(1)).close();
    }

    private void givenEveryMelodyIsValid() {
        Mockito
                .when(diskJokey.isMelodyValid(Mockito.any()))
                .thenReturn(true);

    }
}