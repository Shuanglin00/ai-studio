package com.shuanglin.executor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.shuanglin.framework.annotation.GroupMessageHandler;
import com.shuanglin.framework.bus.event.GroupMessageEvent;
import io.github.admin4j.http.util.HttpJsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;


@Component
@Slf4j
public class PigGroupMessageExecutor {

	/*
	 {"self_id":2784152733,"user_id":1751649231,"time":1752415645,"message_id":375834917,"message_seq":22624,"message_type":"group","sender":{"user_id":1751649231,"nickname":"双零","card":"","role":"owner","title":""},"raw_message":"渚","font":14,"sub_type":"normal","message":"渚","message_format":"string","post_type":"message","group_id":345693826}
	 */
	@GroupMessageHandler(startWith = "渚")
	public void pigGroupMessage(GroupMessageEvent group) throws IOException {
		String text = "";
		group.setMessage(group.getMessage().replace("#pig", ""));
		System.out.println("group = " + group.getGroupId());
		System.out.println("message = " + group.getMessage());
		if (group.getMessage() != null) {
			text = group.getMessage().replace("\"", "");
		}

		String images = getRandomImageAsBase64("C:\\project\\ai-studio\\bot\\src\\main\\resources\\pigs");

		if (text.equals("渚")) {
			JsonObject data1 = new JsonObject();
			data1.addProperty("file", images);
			JsonArray messages = new JsonArray();
			JsonObject jsonObject1 = new JsonObject();
			jsonObject1.addProperty("type", "image");
			jsonObject1.add("data", data1);
			messages.add(jsonObject1);
			JsonObject body = new JsonObject();
			body.add("message", messages);
			body.addProperty("group_id", group.getGroupId());
			HttpJsonUtil.post("http://127.0.0.1:3000/send_group_msg", body.toString());
		}
	}

	public void noticePig(GroupMessageEvent group) throws IOException {
		String text = "";
		group.setMessage(group.getMessage().replace("#pig", ""));
		System.out.println("group = " + group.getGroupId());
		System.out.println("message = " + group.getMessage());
		if (group.getMessage() != null) {
			text = group.getMessage().replace("\"", "");
		}

		int i = new Random().nextInt(69) + 1;
		String images = getRandomImageAsBase64("C:\\project\\ai-studio\\bot\\src\\main\\resources\\pigs");

		if (text.equals("渚")) {
			JsonObject data1 = new JsonObject();
			data1.addProperty("file", images);
			JsonArray messages = new JsonArray();
			JsonObject jsonObject1 = new JsonObject();
			jsonObject1.addProperty("type", "image");
			jsonObject1.add("data", data1);
			messages.add(jsonObject1);
			JsonObject body = new JsonObject();
			body.add("message", messages);
			body.addProperty("group_id", group.getGroupId());
			HttpJsonUtil.post("http://127.0.0.1:3000/send_group_msg", body.toString());
		}
	}

	public static String getRandomImageAsBase64(String directoryPath) {
		File directory = new File(directoryPath);

		// 检查目录是否存在且是否为目录
		if (!directory.exists() || !directory.isDirectory()) {
			System.err.println("错误：目录不存在或不是一个有效的目录 -> " + directoryPath);
			return null;
		}

		// 使用文件名过滤器筛选支持的图片格式
		FilenameFilter imageFilter = (dir, name) -> {
			String lowerCaseName = name.toLowerCase();
			return lowerCaseName.endsWith(".jpg") || lowerCaseName.endsWith(".png") || lowerCaseName.endsWith(".gif") || lowerCaseName.endsWith(".jpeg");
		};

		File[] imageFiles = directory.listFiles(imageFilter);

		if (imageFiles == null || imageFiles.length == 0) {
			System.err.println("错误：在目录下未找到图片文件。");
			return null;
		}

		// 随机选择一个文件
		Random rand = new Random();
		File randomImageFile = imageFiles[rand.nextInt(imageFiles.length)];

		System.out.println("正在读取图片: " + randomImageFile.getAbsolutePath());

		try {
			// 读取文件所有字节
			byte[] fileContent = Files.readAllBytes(randomImageFile.toPath());
			// 使用Java内置的Base64编码器进行编码
			return "base64://"+Base64.getEncoder().encodeToString(fileContent);
		} catch (IOException e) {
			System.err.println("读取文件时出错: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}


}
