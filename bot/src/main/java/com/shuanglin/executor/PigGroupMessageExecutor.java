package com.shuanglin.executor;

import com.shuanglin.aop.annotations.handler.GroupMessageHandler;
import com.shuanglin.aop.event.GroupMessageEvent;
import org.springframework.stereotype.Component;

@Component
public class PigGroupMessageExecutor {

	/*
	 {"self_id":2784152733,"user_id":1751649231,"time":1752415645,"message_id":375834917,"message_seq":22624,"message_type":"group","sender":{"user_id":1751649231,"nickname":"双零","card":"","role":"owner","title":""},"raw_message":"渚","font":14,"sub_type":"normal","message":"渚","message_format":"string","post_type":"message","group_id":345693826}
	 */
	@GroupMessageHandler
	public void pigGroupMessage(GroupMessageEvent group) {
		String text = "";
		String noticeType = "";
		System.out.println("group = " + group);
		System.out.println("message = " + message);
//		if (jsonObject.get("message") != null) {
//			text = jsonObject.get("message").toString().replace("\"", "");
//		}
//		if (jsonObject.get("notice_type") != null) {
//			noticeType = jsonObject.get("notice_type").toString().replace("\"", "");
//		}
//
//		int i = new Random().nextInt(69) + 1;
//		String images = encodeImageToBase64("C:\\project\\ai-studio\\bot\\src\\main\\resources\\pigs" + File.separator + i + ".jpg", true, true);
//
//		if (noticeType.equalsIgnoreCase("notify") || text.equals("渚")) {
//			groupId =jsonObject.get("group_id").getAsString();
//			JsonObject data1 = new JsonObject();
//			data1.addProperty("file", images);
//			JsonArray messages = new JsonArray();
//			JsonObject jsonObject1 = new JsonObject();
//			jsonObject1.addProperty("type", "image");
//			jsonObject1.add("data", data1);
//			messages.add(jsonObject1);
//			JsonObject body = new JsonObject();
//			body.add("message", messages);
//			body.addProperty("group_id", groupId);
//			HttpJsonUtil.post("http://127.0.0.1:3000/send_group_msg", body.toString());
//		}
	}
}
