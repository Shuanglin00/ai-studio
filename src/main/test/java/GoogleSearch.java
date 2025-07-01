//import com.shuanglin.bot.ChatStart;
//import com.shuanglin.bot.langchain4j.assistant.GeminiAssistant;
//import dev.langchain4j.community.web.search.searxng.SearXNGWebSearchEngine;
//import dev.langchain4j.data.document.Document;
//import dev.langchain4j.data.document.DocumentParser;
//import dev.langchain4j.data.document.DocumentSplitter;
//import dev.langchain4j.data.document.parser.TextDocumentParser;
//import dev.langchain4j.data.document.splitter.DocumentSplitters;
//import dev.langchain4j.data.embedding.Embedding;
//import dev.langchain4j.data.segment.TextSegment;
//import dev.langchain4j.memory.chat.MessageWindowChatMemory;
//import dev.langchain4j.model.chat.ChatModel;
//import dev.langchain4j.model.embedding.EmbeddingModel;
//import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
//import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
//import dev.langchain4j.rag.DefaultRetrievalAugmentor;
//import dev.langchain4j.rag.RetrievalAugmentor;
//import dev.langchain4j.rag.content.retriever.ContentRetriever;
//import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
//import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
//import dev.langchain4j.rag.query.router.DefaultQueryRouter;
//import dev.langchain4j.rag.query.router.QueryRouter;
//import dev.langchain4j.service.AiServices;
//import dev.langchain4j.store.embedding.EmbeddingStore;
//import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
//import dev.langchain4j.web.search.WebSearchEngine;
//import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
//import org.springframework.boot.SpringApplication;
//
//import javax.net.ssl.*;
//import java.nio.file.Path;
//import java.security.KeyManagementException;
//import java.security.NoSuchAlgorithmException;
//import java.security.cert.X509Certificate;
//import java.util.List;
//
//import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
//
//public class GoogleSearch {
//	// 在main方法的最开始调用这个方法
//	private static void trustAllHosts() {
//		try {
//			TrustManager[] trustAllCerts = new TrustManager[]{
//					new X509TrustManager() {
//						public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
//						public void checkClientTrusted(X509Certificate[] certs, String authType) {}
//						public void checkServerTrusted(X509Certificate[] certs, String authType) {}
//					}
//			};
//
//			SSLContext sc = SSLContext.getInstance("TLS");
//			sc.init(null, trustAllCerts, new java.security.SecureRandom());
//			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//
//			// Create all-trusting host name verifier
//			HostnameVerifier allHostsValid = (hostname, session) -> true;
//			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
//		} catch (NoSuchAlgorithmException | KeyManagementException e) {
//			throw new RuntimeException("Failed to set trust all hosts", e);
//		}
//	}
//	public static void main(String[] args) {
//		trustAllHosts();
//		String proxyHost = "127.0.0.1";
//		String proxyPort = "7897"; // 代理端口通常是数字，所以用字符串
//
//		System.setProperty("http.proxyHost", proxyHost);
//		System.setProperty("http.proxyPort", proxyPort);
//
//		// 如果您的代理也需要为 HTTPS 流量服务（Google API 使用 HTTPS），也设置这些属性
//		System.setProperty("https.proxyHost", proxyHost);
//		System.setProperty("https.proxyPort", proxyPort);
//		GeminiAssistant assistant = createAssistant();
//
//		String chat = assistant.chat("lin", "你是谁");
//		System.out.println("chat = " + chat);
//	}
//
//	private static GeminiAssistant createAssistant() {
//
//		// Let's create our embedding store content retriever.
//		EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
//
//		EmbeddingStore<TextSegment> embeddingStore =
//				embed(Path.of("D:\\project\\ai-studio\\src\\main\\resources\\documents\\miles-of-smiles-terms-of-use.txt"), embeddingModel);
//
//		ContentRetriever embeddingStoreContentRetriever = EmbeddingStoreContentRetriever.builder()
//				.embeddingStore(embeddingStore)
//				.embeddingModel(embeddingModel)
//				.maxResults(2)
//				.minScore(0.6)
//				.build();
//
////		// Let's create our web search content retriever.
////		WebSearchEngine webSearchEngine = SearXNGWebSearchEngine.builder()
////				.baseUrl(properties.getBaseUrl())
////				.optionalParams("google")
////				.logRequests(true)
////				.logResponses(true)
////				.build();
//
//		ContentRetriever webSearchContentRetriever = WebSearchContentRetriever.builder()
//				.webSearchEngine(webSearchEngine)
//				.maxResults(3)
//				.build();
//
//		// Let's create a query router that will route each query to both retrievers.
//		QueryRouter queryRouter = new DefaultQueryRouter(embeddingStoreContentRetriever, webSearchContentRetriever);
//
//		RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
//				.queryRouter(queryRouter)
//				.build();
//
//		ChatModel model = GoogleAiGeminiChatModel.builder()
//				.apiKey("AIzaSyDf8AumGRKxpZwWGVTYsr3hlxeXZPQ9quQ")
//				.modelName("gemini-2.0-flash")
//				.build();
//
//		return AiServices.builder(GeminiAssistant.class)
//				.chatModel(model)
//				.retrievalAugmentor(retrievalAugmentor)
//				.chatMemory(MessageWindowChatMemory.withMaxMessages(10))
//				.build();
//	}
//
//	private static EmbeddingStore<TextSegment> embed(Path documentPath, EmbeddingModel embeddingModel) {
//		DocumentParser documentParser = new TextDocumentParser();
//		Document document = loadDocument(documentPath, documentParser);
//
//		DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
//		List<TextSegment> segments = splitter.split(document);
//
//		List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
//
//		EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
//		embeddingStore.addAll(embeddings, segments);
//		return embeddingStore;
//	}
//}