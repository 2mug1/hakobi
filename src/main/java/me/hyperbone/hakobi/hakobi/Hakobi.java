package me.hyperbone.hakobi.hakobi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.hyperbone.hakobi.hakobi.letter.Letter;
import me.hyperbone.hakobi.hakobi.letter.handler.IncomingLetterHandler;
import me.hyperbone.hakobi.hakobi.letter.handler.LetterExceptionHandler;
import me.hyperbone.hakobi.hakobi.letter.listener.LetterListener;
import me.hyperbone.hakobi.hakobi.letter.listener.LetterListenerData;
import org.bukkit.plugin.Plugin;
import org.json.JSONException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

public class Hakobi {
    private final Plugin plugin;

    private final String channel;
    private final JedisPool jedisPool;
    private final String password;
    private final Gson gson;

    private JedisPubSub jedisPubSub;
    private final Map<String, List<LetterListenerData>> listeners = new HashMap<>();

    public Hakobi(Plugin plugin, String channel, JedisPool jedisPool, Gson gson) {
        this.plugin = plugin;
        this.channel = channel;
        this.jedisPool = jedisPool;
        this.gson = gson;
        this.password = null;

        this.setupPubSub();
    }

    public Hakobi(Plugin plugin, String channel, JedisPool jedisPool, String password, Gson gson) {
        this.plugin = plugin;
        this.channel = channel;
        this.jedisPool = jedisPool;
        this.password = password;
        this.gson = gson;

        this.setupPubSub();
    }

    public void sendLetter(Letter letter) {
        sendLetter(letter, new LetterExceptionHandler());
    }

    public void sendLetter(Letter letter, LetterExceptionHandler exceptionHandler) {
        try (Jedis client = jedisPool.getResource()) {
            if (this.password != null) {
                client.auth(this.password);
            }
            client.publish(channel, letter.getId() + ";" + gson.toJsonTree(letter.getData()).toString());
        } catch (Exception e) {
            exceptionHandler.onException(e);
        }
    }

    public void registerListener(LetterListener letterListener) {
        for (Method method : letterListener.getClass().getDeclaredMethods()) {
            if (method.getDeclaredAnnotation(IncomingLetterHandler.class) != null && method.getParameters().length != 0) {
                if (!JsonObject.class.isAssignableFrom(method.getParameters()[0].getType())) {
                    throw new IllegalStateException("最初のパラメータはJsonObjectである必要があります。");
                }
                String letterId = method.getDeclaredAnnotation(IncomingLetterHandler.class).value();
                listeners.putIfAbsent(letterId, new ArrayList<>());
                listeners.get(letterId).add(new LetterListenerData(letterListener, method, letterId));
            }
        }
    }

    public void close() {
        if (jedisPubSub != null && jedisPubSub.isSubscribed()) {
            jedisPubSub.unsubscribe();
        }

        jedisPool.close();
    }

    public void setupPubSub() {

        this.jedisPubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (channel.equalsIgnoreCase(Hakobi.this.channel)) {
                    try {
                        int breakAt = message.indexOf(';');
                        String letterId = message.substring(0, breakAt);

                        if (Hakobi.this.listeners.containsKey(letterId)) {
                            JsonObject letterData = gson.fromJson(message.substring(breakAt + 1), JsonObject.class);

                            for (LetterListenerData listener : Hakobi.this.listeners.get(letterId)) {
                                listener.getMethod().invoke(listener.getInstance(), letterData);
                            }
                        }
                    } catch (JSONException ex) {
                        plugin.getLogger().warning("[Hakobi] JSONの読み取りに失敗しました。");
                    } catch (Exception ex) {
                        plugin.getLogger().warning("[Hakobi] 手紙の処理に失敗しました。");
                        ex.printStackTrace();
                    }
                }
            }
        };

        ForkJoinPool.commonPool().execute(() -> {
            try (Jedis client = jedisPool.getResource()) {
                if (this.password != null) {
                    client.auth(this.password);
                }
                client.subscribe(jedisPubSub, channel);
                plugin.getLogger().warning("[Hakobi] チャンネルの登録が完了しました。");
            } catch (Exception ex) {
                plugin.getLogger().warning("[Hakobi] チャンネルの登録に失敗しました。");
                ex.printStackTrace();
            }
        });
    }
}
