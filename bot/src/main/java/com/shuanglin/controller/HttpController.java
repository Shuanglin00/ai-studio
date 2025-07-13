package com.shuanglin.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.shuanglin.aop.RawMessageRouter;
import com.shuanglin.bot.langchain4j.assistant.GeminiAssistant;
import com.shuanglin.bot.langchain4j.config.DocumentInitializer;
import io.github.admin4j.http.HttpRequest;
import io.github.admin4j.http.util.HttpJsonUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Random;

import static ai.djl.repository.FilenameUtils.getFileExtension;

@RestController
@RequestMapping("/bot")
@Slf4j
public class HttpController {
	@Resource
	GeminiAssistant assistant;
	@Resource
	DocumentInitializer documentInitializer;
	@Autowired
	private RawMessageRouter router;

	@PostMapping("")
	public void post(HttpRequest request, @RequestBody String message) throws IOException {
		// 解析Id groupId module
		System.out.println("message = " + message);
		String senderID = "";
		String groupId = "";
		JsonObject jsonObject = new Gson().fromJson(message, JsonObject.class);
		try {
			senderID = jsonObject.getAsJsonObject("sender").get("user_id").getAsString();
			groupId = jsonObject.get("group_id").getAsString();
			if (senderID != null && !senderID.isEmpty()) {
				request.header("sender", senderID);
				request.header("groupId", groupId);
				String send = jsonObject.get("message").getAsString();
				if (send != null && send.startsWith("#chat")) {
					String content = assistant.chat("123", "", senderID, jsonObject.get("message").getAsString());
					JsonObject data1 = new JsonObject();
					data1.addProperty("text", content);
					JsonArray messages = new JsonArray();
					JsonObject jsonObject1 = new JsonObject();
					jsonObject1.addProperty("type", "text");
					jsonObject1.add("data", data1);
					messages.add(jsonObject1);
					JsonObject body = new JsonObject();
					body.add("message", messages);
					body.addProperty("group_id", groupId);
					HttpJsonUtil.post("http://127.0.0.1:3000/send_group_msg", body.toString());
				}
				if (send != null && send.startsWith("#learn")) {
					documentInitializer.learnStr(request,send);
					JsonObject data1 = new JsonObject();
					data1.addProperty("text", "学会了");
					JsonArray messages = new JsonArray();
					JsonObject jsonObject1 = new JsonObject();
					jsonObject1.addProperty("type", "text");
					jsonObject1.add("data", data1);
					messages.add(jsonObject1);
					JsonObject body = new JsonObject();
					body.add("message", messages);
					body.addProperty("group_id", groupId);
					HttpJsonUtil.post("http://127.0.0.1:3000/send_group_msg", body.toString());
				}
			}
		} catch (Exception e) {
			log.info(e.getMessage());
		}
		router.route(message);
	}

	/**
	 * 将图片文件转换为 Base64 字符串。
	 * 可以选择是否添加 Data URI 前缀 (e.g., "data:image/png;base64,")。
	 * 也可以选择添加自定义前缀 (e.g., "base64://")。
	 *
	 * @param imagePath            图片文件的完整路径。
	 * @param includeDataUriPrefix 是否包含标准的 Data URI 前缀 (data:image/type;base64,)
	 * @param includeCustomPrefix  是否包含自定义的 "base64://" 前缀 (如果为 true, 则忽略 includeDataUriPrefix)
	 * @return 转换后的 Base64 字符串，可能带有前缀。
	 * @throws IOException              如果图片文件读取失败或文件不存在。
	 * @throws IllegalArgumentException 如果文件路径无效。
	 */
	public String encodeImageToBase64(String imagePath, boolean includeDataUriPrefix, boolean includeCustomPrefix) throws IOException {
		File file = new File(imagePath);
		if (!file.exists() || !file.isFile()) {
			log.error("图片文件不存在或不是一个文件: {}", imagePath);
			throw new IOException("图片文件不存在或不是一个文件: " + imagePath);
		}

		try (FileInputStream imageInFile = new FileInputStream(file)) {
			byte[] imageData = new byte[(int) file.length()];
			imageInFile.read(imageData);

			String base64String = Base64.getEncoder().encodeToString(imageData);

			// 获取文件 MIME 类型，用于 Data URI 前缀
			String contentType = Files.probeContentType(Paths.get(imagePath));
			if (contentType == null) {
				// 尝试根据文件扩展名猜测 MIME 类型
				String fileExtension = getFileExtension(imagePath);
				switch (fileExtension) {
					case "png":
						contentType = "image/png";
						break;
					case "jpg":
					case "jpeg":
						contentType = "image/jpeg";
						break;
					case "gif":
						contentType = "image/gif";
						break;
					case "bmp":
						contentType = "image/bmp";
						break;
					default:
						contentType = "application/octet-stream"; // 默认通用二进制流
				}
				log.warn("无法通过 Files.probeContentType 获取文件 {} 的 MIME 类型，尝试猜测为: {}", imagePath, contentType);
			}

			if (includeCustomPrefix) {
				return "base64://" + base64String;
			} else if (includeDataUriPrefix) {
				// 标准 Data URI 格式：data:MIME_type;base64,
				return "data:" + contentType + ";base64," + base64String;
			} else {
				return base64String;
			}
		}
	}

	/**
	 * 辅助方法：获取文件扩展名
	 */
	private String getFileExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
			return fileName.substring(dotIndex + 1).toLowerCase();
		}
		return "";
	}

}
