import com.shuanglin.ChatStart;
import com.shuanglin.bot.service.GraphService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
@SpringBootTest(classes = ChatStart.class)
public class AIGraphStorage {
	@Resource
	GraphService graphService;
	@Test
	public void readTestGraph() {
		graphService.readStory();
	}
}