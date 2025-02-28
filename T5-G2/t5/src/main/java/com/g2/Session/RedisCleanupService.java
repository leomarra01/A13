package com.g2.Session;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RedisCleanupService implements DisposableBean {

    private final RedisTemplate<String, Sessione> template;
    private final JedisConnectionFactory jedisConnectionFactory;

    @Autowired
    public RedisCleanupService(RedisTemplate<String, Sessione> template, JedisConnectionFactory jedisConnectionFactory) {
        this.template = template;
        this.jedisConnectionFactory = jedisConnectionFactory;
    }

    @Override
    public void destroy() throws Exception {
        RedisConnection connection = null;
        try {
            // Verifica se il connection factory è attivo, altrimenti lo avvia
            if (!jedisConnectionFactory.isRunning()) {
                jedisConnectionFactory.start();
            }
            connection = jedisConnectionFactory.getConnection();
            connection.serverCommands().flushDb();
            System.out.println("✅ Tutte le sessioni sono state rimosse da Redis");
        } catch (Exception e) {
            System.err.println("Errore di pulizia sessioni: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
}
