package com.shuanglin.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Stack;

public class JsonUtils {

	/**
	 * 扁平化json
	 *
	 * @param sourceObject 源对象
	 * @return {@code JsonObject }
	 */
	public static JsonObject flatten(JsonObject sourceObject) {
		// 最终返回的扁平化对象
		JsonObject flattenedObject = new JsonObject();

		// 创建一个栈，用于存放待处理的 JsonObject
		Stack<JsonObject> processingStack = new Stack<>();

		// 将最外层的源对象首先压入栈中
		processingStack.push(sourceObject);

		// 当栈中还有待处理的对象时，循环继续
		while (!processingStack.isEmpty()) {
			// 从栈中弹出一个对象进行处理
			JsonObject currentObject = processingStack.pop();

			// 遍历当前对象的所有属性
			for (Map.Entry<String, JsonElement> entry : currentObject.entrySet()) {
				String key = entry.getKey();
				JsonElement value = entry.getValue();

				// 检查属性的值是否是另一个 JsonObject
				if (value.isJsonObject()) {
					// 如果是，则将这个嵌套的 JsonObject 压入栈中，以便后续处理
					processingStack.push(value.getAsJsonObject());
				} else {
					// 如果值不是 JsonObject（即是原始值、数组或 null），
					// 这就是我们想要的“叶子节点”。
					// 直接将其原始的键和值添加到最终的扁平化对象中。
					// 注意：如果键已存在，此操作会覆盖旧值。
					flattenedObject.add(key, value);
				}
			}
		}

		return flattenedObject;
	}
}
