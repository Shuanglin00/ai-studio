import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.shuanglin")
public class StartBot {
	public static void main(String[] args) {
		String proxyHost = "127.0.0.1";
		String proxyPort = "7897"; // 代理端口通常是数字，所以用字符串

		System.setProperty("http.proxyHost", proxyHost);
		System.setProperty("http.proxyPort", proxyPort);

		// 如果您的代理也需要为 HTTPS 流量服务（Google API 使用 HTTPS），也设置这些属性
		System.setProperty("https.proxyHost", proxyHost);
		System.setProperty("https.proxyPort", proxyPort);
		SpringApplication.run(StartBot.class, args);
	}
}
