package com.shuanglin.bot.langchain4j.controller;

import cn.hutool.core.util.IdUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shuanglin.bot.langchain4j.assistant.GeminiAssistant;
import com.shuanglin.bot.langchain4j.assistant.OllamaAssistant;
import com.shuanglin.bot.langchain4j.config.DocumentInitializer;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/chat")
public class ChatController {

	@Autowired
	private GeminiAssistant geminiAssistant;

	@Autowired
	private OllamaAssistant ollamaAssistant;

	@Resource
	private DocumentInitializer documentInitializer;

	@Resource
	Gson gson;

	@PostMapping("/ask")
	public String ask(@RequestBody String message) {
		JsonObject params = gson.fromJson(message,JsonObject.class).getAsJsonObject();
		// 日志入口
		params.addProperty("messageId",IdUtil.getSnowflakeNextIdStr());
		String answer= ollamaAssistant.chat(params,params.get("message").getAsString());
		return answer;
	}

	@PostMapping("/readFile")
	public void readDocumentFromStream(@RequestParam("file") MultipartFile multiFile) {
		try {

			// 获取文件名
			String fileName = multiFile.getOriginalFilename();
			// 获取文件后缀
			assert fileName != null;
			String prefix = fileName.substring(fileName.lastIndexOf("."));
			// 若需要防止生成的临时文件重复,可以在文件名后添加随机码
			File file = File.createTempFile(fileName, prefix);
			multiFile.transferTo(file);

			String s = documentInitializer.readFile(new JsonObject(),file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@PostMapping("/read")
	public String read(@RequestBody String body) {
		JsonObject params = new JsonObject();
		params.addProperty("userId", "1751649231");
		params.addProperty("modelName", "123");
		documentInitializer.read(params, body);
		return "OK";
	}
}
