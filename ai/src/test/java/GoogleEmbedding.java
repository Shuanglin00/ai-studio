//import dev.langchain4j.data.message.TextContent;
//import dev.langchain4j.data.message.UserMessage;
//import dev.langchain4j.model.chat.response.ChatResponse;
//import dev.langchain4j.model.embedding.EmbeddingModel;
//
//public class GoogleEmbedding {
//	private static final String PROJECT_ID = "tensile-yen-464506-a5";
//	private static final String MODEL_NAME = "text-multilingual-embedding-002";
//
//	public static void main(String[] args) {
//		EmbeddingModel embeddingModel = VertexAiEmbeddingModel.builder()
//				.project(PROJECT_ID)
//				.location("us-central1")
//				.endpoint("us-central1-aiplatform.googleapis.com:443")
//				.publisher("google")
//				.modelName(MODEL_NAME)
//				.build();
//
//		Response<Embedding> response = embeddingModel.embed("Hello, how are you?");
//
//		Embedding embedding = response.content();
//
//		int dimension = embedding.dimension(); // 768
//		float[] vector = embedding.vector(); // [-0.06050122, -0.046411075, ...
//
//		System.out.println(dimension);
//		System.out.println(embedding.vectorAsList());
//	}
//}
