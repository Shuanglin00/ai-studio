import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;

public class QwenEmbedding {
	public static void main(String[] args) {
		EmbeddingModel model = QwenEmbeddingModel.builder()
				.apiKey("sk-ef837a5f684c4ed087802c948857df5f")
				.modelName("text-embedding-v2")
				.build();
		Embedding content = model.embed("你好，世界！这是一个测试。").content();
		System.out.println("embeddings = " + content);
	}
}
