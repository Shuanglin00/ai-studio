import com.shuanglin.ChatStart;
import com.shuanglin.bot.service.LikeService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ChatStart.class)
public class Aiknowledge {

	@Resource
	LikeService likeService;
	@Test
	public void test(){
		likeService.loadKnowledge();
	}
	@Test
	public void like(){
		likeService.chat("""
退了所有的群。唯独这个群没退，就因为群主答应中秋节给我送一箱大闸蟹，一盒月饼，一杯拿铁，外加一部iPhone17pro max2Tb esim版
				""");

	}
}
