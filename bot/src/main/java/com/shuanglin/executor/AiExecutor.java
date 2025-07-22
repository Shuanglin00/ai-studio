package com.shuanglin.executor;

import com.shuanglin.dbModel.models.ModelsRepository;
import com.shuanglin.dbModel.permission.PermissionRepository;
import com.shuanglin.framework.annotation.GroupMessageHandler;
import com.shuanglin.framework.bus.event.GroupMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AiExecutor {
	@Autowired
	private ModelsRepository modelsRepository;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@GroupMessageHandler(startWith = "#chat" )
	public void chat(GroupMessageEvent group){
		//1. 获取当前用户信息

	}
}
