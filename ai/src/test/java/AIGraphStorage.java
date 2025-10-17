import com.shuanglin.ChatStart;
import com.shuanglin.bot.service.GraphService;
import com.shuanglin.dao.novel.store.Chapter;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Arrays;
import java.util.List;

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
	
	/**
	 * 测试大纲分析功能
	 */
	@Test
	public void testOutlineAnalysis() {
		String outline = "主角韩立在厉飞雨的推荐下想要学习眨眼剑法这门剑法，请求历师兄抄录这本功法。同时韩立将长春功修炼至第五层。";
		String result = graphService.analyzeOutlineForRAG(outline);
		System.out.println("大纲分析结果 = " + result);
	}
	
	/**
	 * 测试章节内容生成功能
	 */
	@Test
	public void testChapterGeneration() {
		String outline = "韩立终于突破了修炼瓶颈，达到了筑基期。他决定离开太南小会，前往更广阔的修仙世界探寻机缘。";
		String relationContext = "韩立与吴风、限小剑关系较好，与厅师叔有深厚师徒情谊。";
		String lastChapterSummary = "上一章中，韩立刚刚完成了一次关键的修炼突破。";
		
		// 使用新的三参数方法
		String chapterContent = graphService.generateNewChapterContent(outline, relationContext, lastChapterSummary);
		System.out.println("生成的章节内容 = " + chapterContent);
	}
	
	/**
	 * 测试大纲引导功能
	 */
	@Test
	public void testOutlineGuidance() {
		String userOutline = "韩立想要提升实力";
		String novelContext = "韩立目前是太南小会的弟子，修为为炼气期第五层";
		String historicalContext = "韩立之前学会了眨眼剑法，与厉师兄关系不错";
		
		String guidance = graphService.guideOutlineOptimization(userOutline, novelContext, historicalContext);
		System.out.println("大纲引导结果 = " + guidance);
	}
	
	/**
	 * 测试具体的大纲分析结果处理
	 */
	@Test
	public void testSpecificRAGAnalysis() {
		// 使用你提供的具体分析结果
		String ragAnalysisJson = """
				{
				  "relationQueries": [
				    "厉飞雨与韩立的关系",
				    "厉飞雨的身份与地位",
				    "眨眼剑法：剑法的来源、特点、等级",
				    "历师兄：身份、地位、与韩立的关系",
				    "长春功：功法等级划分、修炼效果、修炼方法"
				  ]
				}
				""";
		
		// 测试JSON解析
		System.out.println("原始RAG分析结果:");
		System.out.println(ragAnalysisJson);
		
		// 模拟完整的动态搜索流程
		String outline = "韩立在厉飞雨的推荐下想要学习眨眼剑法这门剑法，请求历师兄抄录这本功法。同时韩立将长春功修炼至第五层。";
		String novelId = "1";
		String lastChapterSummary = "上一章中，韩立刚刚完成了一次重要的修炼突破。";
		
		GraphService.ChapterGenerationResult result = graphService.generateChapterFromOutline(outline, novelId, lastChapterSummary);
		
		System.out.println("\n=== 动态查询结果 ===");
		System.out.println("分析结果: " + result.getRagAnalysis());
		System.out.println("\n关系搜索结果:");
		System.out.println(result.getRelationContext());
		System.out.println("\n最近章节上下文:");
		System.out.println(result.getRecentContext());
		System.out.println("\n生成的章节内容:");
		System.out.println(result.getGeneratedContent());
		
	}
	
	/**
	 * 测试JSON解析功能（包括代码格式）
	 */
	@Test
	public void testJsonParsingWithMarkdown() {
		// 模拟AI返回的代码包装的JSON
		String markdownWrappedJson = """
				```json
				{
				  "relationQueries": [
				    "厉飞雨与韩立的关系",
				    "眨眼剑法是什么样的剑法？",
				    "历师兄的身份和地位",
				    "长春功是什么功法？",
				    "长春功修炼至第五层有什么影响？",
				    "眨眼剑法是否需要历师兄抄录？原因是什么？"
				  ]
				}
				```
				""";
		
		System.out.println("测试代码包装的JSON解析:");
		System.out.println("原始内容:");
		System.out.println(markdownWrappedJson);
		
		// 测试完整的动态查询流程
		String outline = "韩立在厉飞雨的推荐下想要学习眨眼剑法这门剑法，请求历师兄抄录这本功法。同时韩立将长春功修炼至第五层。";
		String novelId = "1";
		String lastChapterSummary = "上一章中，韩立正在修炼间中努力提升自己的修为。";
		
		GraphService.ChapterGenerationResult result = graphService.generateChapterFromOutline(outline, novelId, lastChapterSummary);
		
		System.out.println("\n=== 改进后JSON解析结果 ===");
		System.out.println("分析结果: " + result.getRagAnalysis());
		System.out.println("\n关系搜索结果:");
		System.out.println(result.getRelationContext());
		if (result.getRelationContext().contains("暂无相关关系信息")) {
			System.out.println("⚠️ 注意：可能需要先在Neo4j中添加一些测试数据");
		}
	}
	
	/**
	 * 测试实体提取和Cypher生成的调试功能
	 */
	@Test
	public void testEntityExtractionAndCypherGeneration() {
		String[] testQueries = {
				"厉飞雨与韩立的关系",
				"眨眼剑法是什么样的剑法？",
				"历师兄的身份和地位",
				"长春功：功法等级划分、修炼效果"
		};
		
		System.out.println("测试实体提取和Cypher生成:");
		for (String query : testQueries) {
			System.out.println("\n查询: " + query);
			
			// 测试实体提取
			List<String> entities = graphService.debugExtractEntities(query);
			System.out.println("提取的实体: " + entities);
			
			// 测试Cypher生成
			String cypher = graphService.debugGenerateCypher(query);
			System.out.println("生成的Cypher: " + cypher);
		}
	}
			
	/**
	 * 全面测试详细输出功能 - 显示每个步骤的输入和输出
	 */
	@Test
	public void testDetailedOutputForAllSteps() {
		System.out.println("✨ =========================== 全面测试详细输出功能 ============================ ✨");
		
		// 测试数据准备
		String outline = "韩立在厉飞雨的推荐下想要学习眨眼剑法这门剑法，请求历师兄抄录这本功法。同时韩立将长春功修炼至第五层。";
		String novelId = "1";
		String lastChapterSummary = "上一章中，韩立与厉飞雨在彩霞山脉的隐秘地点进行药物交换，维持着半年的秘密交易。厉飞雨提供止痛药，韩立则传授武功。两人通过交易逐渐成为好友，并在隐秘的山洞小水潭中放松身心。\n" +
						"\n" +
						"韩立希望厉飞雨能传授更适合自己的轻柔武功，但厉飞雨的功法偏向阳刚。韩立暗自苦笑，知道自己学的是对方的“破心法”，并以此为笑柄。厉飞雨劝韩立放弃心法，专注于真功夫，并预想两人未来称霸七玄门的美好前景。韩立婉拒了厉飞雨的建议，并询问是否有不用真气的武学，厉飞雨提及了七绝堂罕见的“眨眼剑法”，但该剑法一直未能有人成功练成。";
		
		System.out.println("📝 测试参数:");
		System.out.println("   大纲: " + outline);
		System.out.println("   小说ID: " + novelId);
		System.out.println("   上一章摘要: " + lastChapterSummary);
		System.out.println();
		
		// 执行完整流程
		GraphService.ChapterGenerationResult result = graphService.generateChapterFromOutline(outline, novelId, lastChapterSummary);
		
		// 输出详细流程日志
		System.out.println("📊 详细流程日志:");
		System.out.println(result.getDetailedLog());
		
		// 输出关键结果
		System.out.println("📈 关键结果汇总:");
		System.out.println("┌" + "─".repeat(50) + "┐");
		System.out.println("│ 1. RAG分析结果长度: " + String.format("%4d", result.getRagAnalysis().length()) + " 字符" + " ".repeat(14) + "│");
		System.out.println("│ 2. 关系搜索结果长度: " + String.format("%4d", result.getRelationContext().length()) + " 字符" + " ".repeat(12) + "│");
		System.out.println("│ 3. 最近章节上下文长度: " + String.format("%4d", result.getRecentContext().length()) + " 字符" + " ".repeat(10) + "│");
		System.out.println("│ 4. 生成章节内容长度: " + String.format("%4d", result.getGeneratedContent().length()) + " 字符" + " ".repeat(12) + "│");
		System.out.println("│ 5. 详细日志长度: " + String.format("%7d", result.getDetailedLog().length()) + " 字符" + " ".repeat(15) + "│");
		System.out.println("└" + "─".repeat(50) + "┘");
		
		// 输出关系搜索的详细结果
		System.out.println();
		System.out.println("🔍 关系搜索详细结果:");
		System.out.println(result.getRelationContext());
		
		// 如果没有找到关系数据，提供调试建议
		if (result.getRelationContext().contains("未找到相关信息") || 
		    result.getRelationContext().contains("暂无相关关系信息")) {
			System.out.println();
			System.out.println("🔧 调试建议:");
			System.out.println("1. 检查Neo4j数据库是否运行: bolt://8.138.204.38:7687");
			System.out.println("2. 确保数据库中有相关的章节数据");
			System.out.println("3. 检查节点的name属性是否包含中文字符");
			System.out.println("4. 可以手动测试Cypher查询: MATCH (n) RETURN n LIMIT 5");
		}
		
		System.out.println();
		System.out.println("result = " + result.getGeneratedContent());
		System.out.println("✨ ========================= 测试完成 ========================= ✨");
	}
	
	/**
	 * 单独测试Cypher生成和实体提取的详细输出
	 */
	@Test
	public void testCypherGenerationDetails() {
		System.out.println("💾 测试Cypher生成和实体提取详细输出");
		System.out.println("=" .repeat(60));
		
		String[] testQueries = {
				"厉飞雨与韩立的关系",
				"眨眼剑法是什么样的剑法？",
				"历师兄的身份和地位",
				"长春功：功法等级划分、修炼效果",
				"长春功修炼至第五层有什么影响？"
		};
		
		for (int i = 0; i < testQueries.length; i++) {
			String query = testQueries[i];
			System.out.println(String.format("\n🔍 测试 %d: %s", i + 1, query));
			System.out.println("-".repeat(40));
			
			// 步骤1: 实体提取
			List<String> entities = graphService.debugExtractEntities(query);
			System.out.println("🏷️  提取的实体: " + entities);
			
			// 步骤2: Cypher生成
			String cypher = graphService.debugGenerateCypher(query);
			System.out.println("💾 生成的Cypher:");
			System.out.println("```cypher");
			System.out.println(cypher);
			System.out.println("```");
			
			// 步骤3: 分析Cypher类型
			String cypherType;
			if (entities.size() == 0) {
				cypherType = "通用模糊查询";
			} else if (entities.size() == 1) {
				cypherType = "单实体关系查询";
			} else {
				cypherType = "多实体关系查询";
			}
			System.out.println("📈 Cypher类型: " + cypherType);
		}
		
		System.out.println("\n" + "=".repeat(60));
	}
		
	/**
	 * 专门测试生成第34章 - 基于第30-33章的上下文
	 */
	@Test
	public void testGenerateChapter34() {
		System.out.println("📚 =================== 生成第34章测试 ====================");
		
		// 测试参数
		String novelId = "1";
		String outline = "韩立在掌握了眨眼剑法后，决定进一步提升自己的修炼实力。他开始研究更深层次的修炼方法。";
		
		System.out.println("📝 第34章创作参数:");
		System.out.println("   小说ID: " + novelId);
		System.out.println("   章节大纲: " + outline);
		
		// 步骤1: 获取第30-33章的上下文
		System.out.println("\n🔍 步骤 1: 获取第30-33章上下文");
		String chaptersContext = graphService.getContextForChapter34(novelId);
		
		System.out.println("📈 获取的上下文长度: " + chaptersContext.length() + " 字符");
		System.out.println("📜 第30-33章上下文内容:");
		System.out.println(chaptersContext);
		
		// 步骤2: 生成第34章内容
		System.out.println("\n✍️ 步骤 2: 生成第34章内容");
		
		// 使用第33章作为上一章摘要
		String lastChapterSummary = "第33章中，韩立已经初步掌握了眨眼剑法的要领，并开始思考如何进一步提升。";
		
		// 执行完整的章节生成流程
		GraphService.ChapterGenerationResult result = graphService.generateChapterFromOutline(outline, novelId, lastChapterSummary);
		
		// 输出生成结果
		System.out.println("🎉 章节生成完成!");
		System.out.println("📈 生成结果统计:");
		System.out.println("┌" + "─".repeat(60) + "┐");
		System.out.println("│ RAG分析结果长度: " + String.format("%8d", result.getRagAnalysis().length()) + " 字符" + " ".repeat(26) + "│");
		System.out.println("│ 关系搜索结果长度: " + String.format("%8d", result.getRelationContext().length()) + " 字符" + " ".repeat(24) + "│");
		System.out.println("│ 章节上下文长度: " + String.format("%10d", result.getRecentContext().length()) + " 字符" + " ".repeat(26) + "│");
		System.out.println("│ 第34章内容长度: " + String.format("%9d", result.getGeneratedContent().length()) + " 字符" + " ".repeat(26) + "│");
		System.out.println("└" + "─".repeat(60) + "┘");
		
		// 输出详细流程日志
		System.out.println("\n📊 详细流程日志:");
		System.out.println(result.getDetailedLog());
		
		// 输出生成的第34章内容
		System.out.println("\n📝 生成的第34章内容:");
		System.out.println("=" .repeat(80));
		System.out.println(result.getGeneratedContent());
		System.out.println("=" .repeat(80));
		
		// 给出保存建议
		System.out.println("\n💾 保存建议:");
		System.out.println("如果对生成的第34章内容满意，可以使用以下方法保存:");
		System.out.println("graphService.processConfirmedChapter(\"第三十四章 XXX\", generatedContent, \"凡人修仙传\", \"1\");");
		
		System.out.println("\n🎆 =================== 第34章生成完成 ====================");
	}
	
	/**
	 * 测试获取特定章节范围的上下文
	 */
	@Test
	public void testGetSpecificChaptersContext() {
		System.out.println("🔍 测试获取特定章节范围的上下文");
		
		String novelId = "1";
		
		// 测试获取第30-33章
		System.out.println("\n1. 获取第30-33章:");
		String context30to33 = graphService.getSpecificChaptersContext(novelId, 30, 33);
		System.out.println("结果长度: " + context30to33.length() + " 字符");
		System.out.println("内容: " + context30to33);
		
		// 测试获取单个章节
		System.out.println("\n2. 获取第1章:");
		String context1 = graphService.getSpecificChaptersContext(novelId, 1, 1);
		System.out.println("结果长度: " + context1.length() + " 字符");
		System.out.println("内容: " + context1);
		
		// 测试不存在的章节
		System.out.println("\n3. 测试不存在的章节范围（第100-105章）:");
		String contextNonExistent = graphService.getSpecificChaptersContext(novelId, 100, 105);
		System.out.println("结果: " + contextNonExistent);
	}
	
	/**
	 * 测试专门为第34章准备的上下文方法
	 */
	@Test
	public void testGetContextForChapter34() {
		System.out.println("📚 测试为第34章准备上下文");
		
		String novelId = "1";
		String context = graphService.getContextForChapter34(novelId);
		
		System.out.println("📈 上下文长度: " + context.length() + " 字符");
		System.out.println("📜 上下文内容:");
		System.out.println(context);
	}
	
	/**
	 * 测试获取最近章节上下文
	 */
	@Test
	public void testGetRecentChaptersContext() {
		String novelId = "1";
		String context = graphService.getSpecificChaptersContext(novelId, 1, 3); // 使用新方法
		System.out.println("最近章节上下文 = " + context);
	}
	
	/**
	 * 测试完整的动态章节生成流程
	 */
	@Test
	public void testDynamicChapterGeneration() {
		String outline = "韩立在地下集市中意外遇到了一个神秘的白衣少女，她正在被几名黑衣人追杀。";
		String novelId = "1";
		String lastChapterSummary = "上一章中，韩立在地下市集中闲逛，购买了一些修炼材料。";
		
		GraphService.ChapterGenerationResult result = graphService.generateChapterFromOutline(outline, novelId, lastChapterSummary);
		System.out.println("动态生成流程结果:");
		System.out.println("RAG分析: " + result.getRagAnalysis());
		System.out.println("关系上下文: " + result.getRelationContext());
		System.out.println("最近章节上下文: " + result.getRecentContext());
		System.out.println("生成内容:");
		System.out.println(result.getGeneratedContent());
	}
	
	/**
	 * 测试用户确认章节处理功能
	 */
	@Test
	public void testProcessConfirmedChapter() {
		String chapterTitle = "第七十二章 地下集市的遇遇";
		String chapterContent = "韩立在地下集市中闲逛，突然听到了打斗声..."; // 粗化内容
		String novelName = "凡人修仙传";
		String novelId = "1";
		
		Chapter processedChapter = graphService.processConfirmedChapter(chapterTitle, chapterContent, novelName, novelId);
		System.out.println("处理后的章节ID: " + processedChapter.getId());
		System.out.println("章节摘要: " + processedChapter.getDescription());
	}

	/**
	 * 测试获取指定章节（例如31, 32, 33）的内容
	 */
	@Test
	public void testGetSpecificChapters() {
		String novelId = "1";
		List<Integer> chapterNumbers = Arrays.asList(31, 32, 33);
		String context = graphService.getSpecificChaptersContextByNumbers(novelId, chapterNumbers);
		System.out.println("指定章节上下文:\n" + context);
	}
}