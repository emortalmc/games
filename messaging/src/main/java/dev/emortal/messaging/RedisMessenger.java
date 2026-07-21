package dev.emortal.messaging;

import dev.emortal.messaging.message.Channel;
import dev.emortal.messaging.message.MessageRegistry;
import dev.emortal.messaging.message.RedisMessage;
import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RedisMessenger {

    private final Map<Class<? extends RedisMessage>, MessageConsumer<RedisMessage>> messageHandlerMap = new HashMap<>();
    private final Map<Class<? extends RedisMessage>, List<CompletableFuture<RedisMessage>>> messageFuturesMap = new HashMap<>();
    private final RedisClient client;
    private final RedisPubSubAsyncCommands<String, String> pubSub;
    public RedisMessenger(String url) {
        this.client = RedisClient.create(url);

        pubSub = client.connectPubSub().async();

        pubSub.subscribe("all");
        pubSub.getStatefulConnection().addListener(new Listener());
    }

    public void listenForChannel(Channel channel) {
        pubSub.subscribe(channel.name().toLowerCase(Locale.ROOT));
    }

    public void listenForServerUUID(UUID uuid) {
        pubSub.subscribe(uuid.toString());
    }

    public CompletableFuture<Long> sendMessage(Channel channel, RedisMessage message) {
        return pubSub.publish(channel.name().toLowerCase(Locale.ROOT), MessageRegistry.encode(message)).toCompletableFuture();
    }

    public CompletableFuture<Long> sendServerMessage(UUID uuid, RedisMessage message) {
        return pubSub.publish(uuid.toString(), MessageRegistry.encode(message)).toCompletableFuture();
    }

    public <T extends RedisMessage> void addMessageHandler(Class<T> clazz, MessageConsumer<T> consumer) {
        messageHandlerMap.put(clazz, (MessageConsumer<RedisMessage>) consumer);
    }

    public <T extends RedisMessage> CompletableFuture<T> awaitMessage(Class<? extends RedisMessage> clazz) {
        CompletableFuture<T> future = new CompletableFuture<>();
        List<CompletableFuture<RedisMessage>> futures = messageFuturesMap.computeIfAbsent(clazz, _ -> new ArrayList<>());
        futures.add((CompletableFuture<RedisMessage>) future);
        return future;
    }

    public interface MessageConsumer<T> {
        void accept(String channel, T msg);
    }

    class Listener implements RedisPubSubListener<String, String> {

        @Override
        public void message(String channel, String message) {
            RedisMessage decode = MessageRegistry.decode(message);
            List<CompletableFuture<RedisMessage>> futures = messageFuturesMap.get(decode.getClass());
            if (futures != null) {
                for (CompletableFuture<RedisMessage> future : futures) {
                    future.complete(decode);
                }
                messageFuturesMap.remove(decode.getClass());
            }

            MessageConsumer<RedisMessage> consumer = messageHandlerMap.get(decode.getClass());
            if (consumer != null) consumer.accept(channel, decode);
        }

        @Override
        public void message(String pattern, String channel, String message) {}
        @Override
        public void subscribed(String channel, long count) {}
        @Override
        public void psubscribed(String pattern, long count) {}
        @Override
        public void unsubscribed(String channel, long count) {}
        @Override
        public void punsubscribed(String pattern, long count) {}
    }

}
