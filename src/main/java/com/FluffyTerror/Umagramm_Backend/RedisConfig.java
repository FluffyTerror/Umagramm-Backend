package com.FluffyTerror.Umagramm_Backend;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public ReactiveRedisTemplate<String,String> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory){
        StringRedisSerializer serializer = new StringRedisSerializer();// так как по дефолту redis работает с типом данных byte[], а мне нужно работать с String, делаю сериализатор
        /*По факту задаю правила как сериализовать ключи и как сериализовать значения
            key   → String → byte[]
            value → String → byte[]*/
        RedisSerializationContext<String, String> context = RedisSerializationContext
                .<String, String>newSerializationContext(serializer)
                .build();
        return new ReactiveRedisTemplate<>(factory, context);//создаётся клиент Redis с моими правилами
    }

    @Bean
    public ReactiveRedisTemplate<String, byte[]> reactiveByteArrayRedisTemplate(ReactiveRedisConnectionFactory factory) {
        RedisSerializationContext<String, byte[]> context = RedisSerializationContext
                .<String, byte[]>newSerializationContext(new StringRedisSerializer()) // Ключи — строки
                .value(RedisSerializer.byteArray()) // Значения — голые байты
                .build();
        return new ReactiveRedisTemplate<>(factory, context);
    }

}