package com.shuanglin.executor;

import cn.hutool.core.util.IdUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.shuanglin.bot.langchain4j.assistant.OllamaAssistant;
import com.shuanglin.dao.GroupInfo;
import com.shuanglin.dao.Model;
import com.shuanglin.dao.ModelsRepository;
import com.shuanglin.dao.SenderInfo;
import com.shuanglin.executor.vo.ChatParam;
import com.shuanglin.framework.annotation.GroupMessageHandler;
import com.shuanglin.framework.bus.event.GroupMessageEvent;
import com.shuanglin.utils.GroupInfoUtil;
import io.github.admin4j.http.util.HttpJsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@RequiredArgsConstructor
public class AiExecutor {

	private final Gson gson;

	private final GroupInfoUtil groupInfoUtil;

	private final OllamaAssistant assistant;

	private final ModelsRepository modelsRepository;

	@GroupMessageHandler(startWith = "#chat")
	public void chat(GroupMessageEvent group) {
		//1. 获取当前用户信息;
		SenderInfo senderInfo = groupInfoUtil.getGroupSenderInfo(group);
		if (!groupInfoUtil.checkModelPermission(group, senderInfo.getModelInfo().getUseModel())) {
			return;
		}
		Model model = modelsRepository.getModelByModelName(senderInfo.getModelInfo().getUseModel());
		String s = assistant.groupChat(gson.toJsonTree(ChatParam.builder().senderInfo(senderInfo).groupMessageEvent(group)).getAsJsonObject(), group.getMessage());
		System.out.println("s = " + s);
		JsonObject data1 = new JsonObject();
		data1.addProperty("text", s);
		JsonArray messages = new JsonArray();
		JsonObject jsonObject1 = new JsonObject();
		jsonObject1.addProperty("type", "text");
		jsonObject1.add("data", data1);
		messages.add(jsonObject1);
		JsonObject body = new JsonObject();
		body.add("message", messages);
		body.addProperty("group_id", group.getGroupId());
		HttpJsonUtil.post("http://127.0.0.1:3000/send_group_msg", body.toString());
	}

	/**
	 * 模型名称
	 * 模型描述
	 * 模型指令
	 *
	 * @param group
	 */
	@GroupMessageHandler(startWith = "#发布模型")
	public void publishModel(GroupMessageEvent group) {
		String[] params = group.getMessage().split(" ");

		//1. 获取当前用户信息;
		SenderInfo senderInfo = groupInfoUtil.getGroupSenderInfo(group);
		GroupInfo groupInfo = groupInfoUtil.getGroupInfo(group);
		Model model = new Model();
		model.setModelName(params[0]);
		model.setConstraints(params[1]);
		model.setInstruction(params[2]);
		model.setIsActive("true");
		model.setId(IdUtil.getSnowflakeNextIdStr());
		model.setConstraints("1. 你必须遵守中华人民共和国法律法规，不得逾越或触碰任何违法甚至损害中国形象。\n" +
				"2. 你必须使用简体中文，或者繁体中文，或者粤语的俚语进行回去，取决于问题所使用语言。\n" +
				"3. 你将扮演多个角色，回答符合角色设定且根据历史记录相关的回答。\n" +
				"4. 回答内容尽可能符合角色设定，字数保持在200以内。");
		groupInfoUtil.publishModel(model);

	}

	@GroupMessageHandler(startWith = "#选择模型")
	public void switchModel(GroupMessageEvent group) {
		String[] params = group.getMessage().split(" ");

		//1. 获取当前用户信息;
		SenderInfo senderInfo = groupInfoUtil.getGroupSenderInfo(group);

		groupInfoUtil.switchModel(senderInfo, params[0]);
	}

}
