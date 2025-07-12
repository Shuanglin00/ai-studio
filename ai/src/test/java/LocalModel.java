import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.localai.LocalAiChatModel;

public class LocalModel {
    public static void main(String[] args) {
        ChatModel model =LocalAiChatModel.builder()
                .baseUrl("http://127.0.0.1:1234/vi/")
                .modelName("google/gemma-3-12b")
                .maxTokens(3)
                .logRequests(true)
                .logResponses(true)
                .build();
        String content = model.chat("你是谁");
        System.out.println("content = " + content);
    }
}
/*
curl http://localhost:1234/v1/chat/completions \
        -H "Content-Type: application/json" \
        -d '{
        "model": "google/gemma-3-12b",
        "messages": [
        { "role": "system", "content": "Always answer in rhymes. Today is Thursday" },
        { "role": "user", "content": "What day is it today?" }
        ],
        "temperature": 0.7,
        "max_tokens": -1,
        "stream": false
        }'*/
