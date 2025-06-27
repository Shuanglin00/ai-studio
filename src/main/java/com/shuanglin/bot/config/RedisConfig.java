package com.shuanglin.bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig {
	@Bean
	public RedisTemplate<String, String> redisTemplate(LettuceConnectionFactory factory) {
		RedisTemplate<String, String> template = new RedisTemplate<>();
		template.setKeySerializer(org.springframework.data.redis.serializer.StringRedisSerializer.UTF_8);
		template.setConnectionFactory(factory);
		return template;
	}
}
