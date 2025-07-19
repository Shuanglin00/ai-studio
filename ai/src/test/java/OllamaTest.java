import dev.langchain4j.model.ollama.OllamaChatModel;

public class OllamaTest {
	public static void main(String[] args) {
		OllamaChatModel chatModel = OllamaChatModel.builder()
				.baseUrl("http://localhost:11434")
				.temperature(0.0) // 模型温度，控制模型生成的随机性，0-1之间，越大越多样性
				.logRequests(true)
				.logRequests(true)
				.logResponses(true)
				.modelName("gemma3:1b")
				.build();
		System.out.println("chatModel.chat(\"你是谁\") = " + chatModel.chat("你是谁"));
	}
}
