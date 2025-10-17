import com.shuanglin.ChatStart;
import com.shuanglin.bot.model.CleanupReport;
import com.shuanglin.bot.model.IsolationMetadata;
import com.shuanglin.bot.model.ProcessReport;
import com.shuanglin.bot.service.ChapterStorageService;
import com.shuanglin.bot.service.GraphService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

/**
 * 章节级知识图谱构建测试
 * 测试读取EPUB前40章并构建知识图谱的完整流程
 */
@SpringBootTest(classes = ChatStart.class)
public class EpubChapterLimitTest {

	@Resource
	private GraphService graphService;

	@Resource
	private ChapterStorageService chapterStorageService;

	/**
	 * 测试读取EPUB前40章并构建知识图谱
	 * 
	 * 前置条件：
	 * 1. 确保EPUB文件存在于指定路径
	 * 2. Neo4j、MongoDB服务已启动
	 * 3. LLM服务（Ollama）已启动并配置
	 */
	@Test
	public void testReadEpub40Chapters() {
		// 步骤1: 准备元数据
		String bookUuid = UUID.randomUUID().toString();
		IsolationMetadata metadata = new IsolationMetadata();
		metadata.setDataSource("test_epub_40");
		metadata.setBookName("斗破苍穹");
		metadata.setBookUuid(bookUuid);
		metadata.setChapterLimit(40);
		metadata.setCreatedBy("test_user");
		metadata.addTag("source", "epub");
		metadata.addTag("version", "v1.0");

		// 步骤2: 验证元数据
		metadata.validate();
		System.out.println("✅ 元数据验证通过: " + metadata);

		// 步骤3: 读取EPUB并构建图谱
		String epubPath = "C:\\Users\\Shuan\\Downloads\\斗破苍穹-天蚕土豆.epub";
		ProcessReport report = graphService.readStoryWithLimit(epubPath, 40, metadata);

		// 步骤4: 输出处理报告
		System.out.println("\n" + report);

		// 步骤5: 查询统计信息
		ChapterStorageService.BookStats stats = chapterStorageService.getBookStatistics(bookUuid);
		System.out.println("\n📊 MongoDB统计信息:");
		System.out.println(stats);

		// 步骤6: 查询Neo4j统计
		String neo4jStats = graphService.queryTestDataStats("test_epub_40");
		System.out.println("\n" + neo4jStats);

		// 验证结果
		assert report.getTotalChapters() == 40 : "应处理40个章节";
		assert report.getSuccessChapters() > 0 : "至少应有成功处理的章节";
		
		System.out.println("\n✅ 测试完成！");
	}

	/**
	 * 测试查询失败章节并重放
	 */
	@Test
	public void testReplayFailedChapters() {
		String bookUuid = "your-book-uuid-here"; // 替换为实际的bookUuid

		// 步骤1: 查询失败章节
		var failedChapters = chapterStorageService.queryFailedChapters(bookUuid);
		System.out.println("📋 失败章节数: " + failedChapters.size());

		// 步骤2: 重放失败章节
		for (var chapter : failedChapters) {
			System.out.println("\n🔄 重放章节 " + chapter.getChapterIndex() + ": " + chapter.getTitle());
			boolean success = graphService.replayCypherFromMongo(bookUuid, chapter.getChapterIndex());
			if (success) {
				System.out.println("✅ 重放成功");
			} else {
				System.out.println("❌ 重放失败");
			}
		}

		System.out.println("\n✅ 重放测试完成！");
	}

	/**
	 * 测试数据清理功能
	 */
	@Test
	public void testCleanupTestData() {
		String dataSource = "test_epub_40";

		// 步骤1: 清理前查询统计
		System.out.println("🔍 清理前数据统计:");
		graphService.queryTestDataStats(dataSource);

		// 步骤2: 执行清理
		CleanupReport report = graphService.cleanupTestData(dataSource);

		// 步骤3: 输出清理报告
		System.out.println("\n" + report);

		// 步骤4: 清理后查询统计（验证是否清理干净）
		System.out.println("\n🔍 清理后数据统计:");
		graphService.queryTestDataStats(dataSource);

		// 验证清理结果
		assert report.getMongoDocsDeleted() > 0 : "应删除MongoDB文档";
		
		System.out.println("\n✅ 清理测试完成！");
	}

	/**
	 * 测试查询指定章节的Cypher语句
	 */
	@Test
	public void testQueryChapterCypher() {
		String bookUuid = "your-book-uuid-here"; // 替换为实际的bookUuid
		Integer chapterIndex = 1;

		// 查询章节
		var chapter = chapterStorageService.queryChapterByIndex(bookUuid, chapterIndex);
		if (chapter != null) {
			System.out.println("📖 章节标题: " + chapter.getTitle());
			System.out.println("📊 处理状态: " + chapter.getProcessStatus());
			System.out.println("🔧 Cypher执行状态: " + chapter.getCypherExecuteStatus());
			System.out.println("\n📝 Cypher语句:");
			System.out.println(chapter.getCypherStatements());
		} else {
			System.out.println("❌ 未找到章节");
		}
	}

	/**
	 * 测试元数据验证
	 */
	@Test
	public void testMetadataValidation() {
		// 测试1: 正常元数据
		IsolationMetadata validMetadata = new IsolationMetadata();
		validMetadata.setDataSource("test_valid");
		validMetadata.setBookName("测试书籍");
		validMetadata.setBookUuid(UUID.randomUUID().toString());
		validMetadata.setChapterLimit(40);
		
		try {
			validMetadata.validate();
			System.out.println("✅ 正常元数据验证通过");
		} catch (Exception e) {
			System.out.println("❌ 验证失败: " + e.getMessage());
		}

		// 测试2: dataSource为空
		IsolationMetadata invalidMetadata1 = new IsolationMetadata();
		invalidMetadata1.setDataSource("");
		invalidMetadata1.setBookName("测试书籍");
		invalidMetadata1.setBookUuid(UUID.randomUUID().toString());
		
		try {
			invalidMetadata1.validate();
			System.out.println("❌ 应该验证失败但没有");
		} catch (IllegalArgumentException e) {
			System.out.println("✅ 正确捕获异常: " + e.getMessage());
		}

		// 测试3: dataSource格式不合法
		IsolationMetadata invalidMetadata2 = new IsolationMetadata();
		invalidMetadata2.setDataSource("test-invalid!");
		invalidMetadata2.setBookName("测试书籍");
		invalidMetadata2.setBookUuid(UUID.randomUUID().toString());
		
		try {
			invalidMetadata2.validate();
			System.out.println("❌ 应该验证失败但没有");
		} catch (IllegalArgumentException e) {
			System.out.println("✅ 正确捕获异常: " + e.getMessage());
		}

		// 测试4: chapterLimit超出范围
		IsolationMetadata invalidMetadata3 = new IsolationMetadata();
		invalidMetadata3.setDataSource("test_valid");
		invalidMetadata3.setBookName("测试书籍");
		invalidMetadata3.setBookUuid(UUID.randomUUID().toString());
		invalidMetadata3.setChapterLimit(2000);
		
		try {
			invalidMetadata3.validate();
			System.out.println("❌ 应该验证失败但没有");
		} catch (IllegalArgumentException e) {
			System.out.println("✅ 正确捕获异常: " + e.getMessage());
		}

		System.out.println("\n✅ 元数据验证测试完成！");
	}

	/**
	 * 测试BookStats统计功能
	 */
	@Test
	public void testBookStatistics() {
		String bookUuid = "your-book-uuid-here"; // 替换为实际的bookUuid

		ChapterStorageService.BookStats stats = chapterStorageService.getBookStatistics(bookUuid);
		
		System.out.println("📊 书籍统计信息:");
		System.out.println("  书籍UUID: " + stats.getBookUuid());
		System.out.println("  总章节数: " + stats.getTotalChapters());
		System.out.println("  已完成: " + stats.getCompletedChapters());
		System.out.println("  失败: " + stats.getFailedChapters());
		System.out.println("  待处理: " + stats.getPendingChapters());
		System.out.println("  成功Cypher: " + stats.getSuccessCypherCount());
		System.out.println("  失败Cypher: " + stats.getFailedCypherCount());
		
		// 计算成功率
		if (stats.getTotalChapters() > 0) {
			double successRate = (double) stats.getCompletedChapters() / stats.getTotalChapters() * 100;
			System.out.println("  成功率: " + String.format("%.2f", successRate) + "%");
		}

		System.out.println("\n✅ 统计测试完成！");
	}
}
