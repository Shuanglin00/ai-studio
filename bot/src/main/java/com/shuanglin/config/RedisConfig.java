package com.shuanglin.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shuanglin.dbModel.info.SenderInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;
import java.util.Map;

@Configuration
public class RedisConfig {

	@Bean("listRedisTemplate")
	public RedisTemplate<String, List<SenderInfo>> listRedisTemplate(RedisConnectionFactory connectionFactory) {
		return createRedisTemplate(connectionFactory, new TypeReference<List<SenderInfo>>() {});
	}

	@Bean("senderInfoRedisTemplate")
	public RedisTemplate<String, Map<String, SenderInfo>> senderInfoRedisTemplate(RedisConnectionFactory connectionFactory) {
		return createRedisTemplate(connectionFactory, new TypeReference<Map<String, SenderInfo>>() {});
	}
	// 在 RedisConfig.java 中定义这个通用的 Bean
	@Bean("groupInfoRedisTemplate")
	public RedisTemplate<String, Object> groupInfoRedisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		StringRedisSerializer stringSerializer = new StringRedisSerializer();
		// Key 和 HashKey 都使用 String 序列化
		template.setKeySerializer(stringSerializer);
		template.setHashKeySerializer(stringSerializer);

		// Value 和 HashValue 都使用通用的 Jackson 序列化器
		Jackson2JsonRedisSerializer<Object> jacksonSerializer = createGenericJacksonSerializer(); // 这是一个包含 ObjectMapper 配置的辅助方法
		template.setValueSerializer(jacksonSerializer);
		template.setHashValueSerializer(jacksonSerializer);

		template.afterPropertiesSet();
		return template;
	}

	// 通用的 Jackson 序列化器创建方法
	private Jackson2JsonRedisSerializer<Object> createGenericJacksonSerializer() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
		objectMapper.registerModule(new JavaTimeModule());
		return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
	}


	private <T> RedisTemplate<String, T> createRedisTemplate(RedisConnectionFactory connectionFactory, TypeReference<T> typeReference) {
		RedisTemplate<String, T> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// --- 序列化器配置 ---
		StringRedisSerializer stringSerializer = new StringRedisSerializer();
		template.setKeySerializer(stringSerializer);
		template.setHashKeySerializer(stringSerializer);

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.activateDefaultTyping(
				LaissezFaireSubTypeValidator.instance,
				ObjectMapper.DefaultTyping.NON_FINAL,
				JsonTypeInfo.As.PROPERTY
		);
		objectMapper.registerModule(new JavaTimeModule());

		JavaType javaType = objectMapper.getTypeFactory().constructType(typeReference);

		// 2. 使用接受 JavaType 的构造函数来创建 Jackson2JsonRedisSerializer
		Jackson2JsonRedisSerializer<T> jacksonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, javaType);

		template.setValueSerializer(jacksonSerializer);
		template.setHashValueSerializer(jacksonSerializer);

		template.afterPropertiesSet();
		return template;
	}
}