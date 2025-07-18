import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.shuanglin")
public class StartBot {
	public static void main(String[] args) {
		SpringApplication.run(StartBot.class, args);
	}
}
