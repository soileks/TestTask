package org;

import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class ServerHandlerTest {

    @Mock
    private ChannelHandlerContext ctx;

    private ServerHandler serverHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Инициализация моков
        serverHandler = new ServerHandler();
    }

    @Test
    void testHandleLoginSuccess() {
        // Подготовка
        String command = "login -u=testUser";

        // Выполнение
        serverHandler.channelRead0(ctx, command);

        // Проверка
        verify(ctx).writeAndFlush("Logged in as testUser\n");
    }

    @Test
    void testHandleLoginInvalidFormat() {
        // Подготовка
        String command = "login invalidFormat";

        // Выполнение
        serverHandler.channelRead0(ctx, command);

        // Проверка
        verify(ctx).writeAndFlush("Invalid login format. Use: login -u=username\n");
    }

    @Test
    void testHandleCreateTopicSuccess() {
        // Очистка состояния сервера перед тестом
        VotingServer.getTopics().clear();

        // Подготовка
        serverHandler.channelRead0(ctx, "login -u=testUser"); // Логиним пользователя
        String command = "create topic -n=testTopic";

        // Выполнение
        serverHandler.channelRead0(ctx, command);

        // Проверка
        verify(ctx).writeAndFlush("Topic created: testTopic\n");
    }

    @Test
    void testHandleCreateTopicAlreadyExists() {
        // Подготовка
        serverHandler.channelRead0(ctx, "login -u=testUser"); // Логиним пользователя
        serverHandler.channelRead0(ctx, "create topic -n=testTopic"); // Создаем топик
        String command = "create topic -n=testTopic"; // Пытаемся создать топик с тем же именем

        // Выполнение
        serverHandler.channelRead0(ctx, command);

        // Проверка
        verify(ctx).writeAndFlush("Topic already exists: testTopic\n");
    }

    @Test
    void testHandleViewTopics() {
        // Подготовка
        serverHandler.channelRead0(ctx, "login -u=testUser"); // Логиним пользователя
        serverHandler.channelRead0(ctx, "create topic -n=testTopic"); // Создаем топик

        // Выполнение
        serverHandler.channelRead0(ctx, "view");

        // Проверка
        verify(ctx).writeAndFlush(contains("testTopic (votes in topic=0)"));
    }

    @Test
    void testHandleVoteSuccess() {
        // Подготовка
        serverHandler.channelRead0(ctx, "login -u=testUser"); // Логиним пользователя
        serverHandler.channelRead0(ctx, "create topic -n=testTopic"); // Создаем топик
        serverHandler.channelRead0(ctx, "create vote -t=testTopic"); // Создаем голосование
        serverHandler.channelRead0(ctx, "TestVote"); // Название голосования
        serverHandler.channelRead0(ctx, "TestDescription"); // Описание голосования
        serverHandler.channelRead0(ctx, "2"); // Количество вариантов
        serverHandler.channelRead0(ctx, "Option1"); // Вариант 1
        serverHandler.channelRead0(ctx, "Option2"); // Вариант 2

        // Выполнение
        serverHandler.channelRead0(ctx, "vote -t=testTopic -v=TestVote"); // Голосуем
        serverHandler.channelRead0(ctx, "1"); // Выбираем вариант 1

        // Проверка
        verify(ctx).writeAndFlush("Vote recorded\n");
    }
    @Test
    void testHandleSaveSuccess() {
        // Подготовка
        String command = "save testFile.txt";

        // Выполнение
        serverHandler.channelRead0(ctx, command);

        // Проверка
        verify(ctx).writeAndFlush(contains("Data saved to testFile.txt"));
    }
    @Test
    void testHandleLoadSuccess() {
        // Подготовка
        String command = "load testFile.txt";

        // Выполнение
        serverHandler.channelRead0(ctx, command);

        // Проверка
        verify(ctx).writeAndFlush(contains("Data loaded from testFile.txt"));
    }


}