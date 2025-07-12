import com.alibaba.fastjson.JSONObject;
import io.github.admin4j.http.util.HttpJsonUtil;

public class LocalEmbedding {
    public static void main(String[] args) throws Exception {
        JSONObject body = new JSONObject();
        body.put("input","你是誰");
        String post = HttpJsonUtil.post("http://192.168.31.148:1234/v1/embeddings", body).toJSONString();
        System.out.println("post.body() = " + post);
    }
}
