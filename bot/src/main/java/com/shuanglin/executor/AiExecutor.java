package com.shuanglin.executor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shuanglin.bot.langchain4j.assistant.GeminiAssistant;
import com.shuanglin.dbModel.info.SenderInfo;
import com.shuanglin.dbModel.models.ModelsRepository;
import com.shuanglin.dbModel.permission.PermissionRepository;
import com.shuanglin.framework.annotation.GroupMessageHandler;
import com.shuanglin.framework.bus.event.GroupMessageEvent;
import com.shuanglin.utils.GroupInfoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiExecutor {
	private ModelsRepository modelsRepository;

	private PermissionRepository permissionRepository;

	private StringRedisTemplate stringRedisTemplate;

	private Gson gson;

	private GroupInfoUtil groupInfoUtil;

	private GeminiAssistant assistant;

	@GroupMessageHandler(startWith = "#chat" )
	public void chat(GroupMessageEvent group){
		//1. 获取当前用户信息;
		SenderInfo senderInfo = groupInfoUtil.getGroupSenderInfo(group);
		groupInfoUtil.checkModelPermission(group,senderInfo.getModelInfo().getUseModel());

		JsonObject params = gson.fromJson(group.getMessage(), JsonObject.class);
		String question = params.get("question").getAsString();
		params.remove("question");
		String s = assistant.groupChat(params, question);
	}
}
