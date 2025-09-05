import com.shuanglin.ChatStart;
import com.shuanglin.bot.service.GraphService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest(classes = ChatStart.class)
public class AIGraphStorage {
	@Resource
	GraphService graphService;
	@Test
	public void readTestGraph() {
		graphService.readStory("C:\\Users\\Shuan\\Downloads\\斗破苍穹-天蚕土豆.epub");
	}
}