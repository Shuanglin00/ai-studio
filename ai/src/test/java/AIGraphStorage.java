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
		graphService.readStory("C:\\Users\\Shuan\\Downloads\\凡人修仙传 - 忘语 - 2511CHS.epub");
	}
	@Test
	public void analyseNovel() {
		graphService.analyseNovel();
	}
	@Test
	public void searchRelationsByOutline() {
		String answer = graphService.searchRelations("主角韩立在厉飞雨的推荐下想要学习眨眼剑法这门剑法，请求历师兄抄录这本功法。同时韩立将长春功修炼至第五层");
		System.out.println("answer = " + answer);
	}
}