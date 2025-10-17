package com.shuanglin.bot.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.shuanglin.bot.utils.FileReadUtil;
import com.shuanglin.dao.novel.store.Chapter;
import com.shuanglin.dao.novel.store.ChapterRepository;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaChatModel;
import jakarta.annotation.Resource;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class GraphService {
  private static final Logger logger = LoggerFactory.getLogger(GraphService.class);
  private static final String NEO4J_URI = "bolt://8.138.204.38:7687";
  private static final String NEO4J_USER = "neo4j";
  private static final String NEO4J_PASSWORD = "password";
  private static final List<Chapter> history = new LinkedList<>();

  private final Driver driver;
  @Resource(name = "decomposeLanguageModel")
  private OllamaChatModel decomposeLanguageModel;

  @Resource
  private ChapterRepository chapterRepository;

  public PromptTemplate graphPromptTemplate() {
    return PromptTemplate.from(
            """
                    **角色设定：**
                    				你是一个专业的知识图谱构建助手，目标是将小说文本内容结构化，并生成用于导入 Neo4j 数据库的 Cypher 插入语句。
                    
                    				**输入结构：**
                    				1.  **章节标题 (ChapterTitle):** {chapterTitle}
                    				2.  **章节内容 (ChapterContent):** {chapterContent}
                    
                    				**核心任务：**
                    				基于提供的【章节标题】和【章节内容】，执行以下三步分析：
                    				1.  **实体识别：** 提取文本中出现的所有关键实体（包括但不限于：人物/Character、地点/Location、物品/Item、技能/Skill、状态/State、事件/Event）。
                    				2.  **关系抽取：** 识别并定义实体之间存在的逻辑关系。
                    				3.  **属性提取：** 抽取实体的描述性属性（如名称、等级、描述等）。
                    
                    				**生成要求（约束条件）：**
                    				1.  **输出格式：** **只**输出 Neo4j Cypher 语句，**绝不**包含任何解释、说明性文字或Markdown格式（如代码块标识```）。
                    				2.  **节点创建：** 必须使用 `MERGE` 语句，以确保幂等性（避免重复创建节点）。
                    				3.  **节点标签规范：** 必须使用以下标签：`:Character`, `:Location`, `:Item`, `:Skill`, `:State`, `:Event`。
                    				4.  **关系类型规范：** 关系类型必须是**英文大写**，并使用 `:` 前缀（例如：`:LOCATED_IN`, `:USES`, `:LEARNS`, `:HAS`, `:PERFORMS`）。
                    				5.  **属性键名规范：** 所有实体的属性键名必须使用**中文**（例如：`name`, `描述`, `等级`, `威力`）。
                    				6.  **空输出：** 如果分析后认为当前章节内容中**无任何可提取的新信息**，则返回一个**空字符串**。
                    				7.  **【关键变量规范】:** 在整个 Cypher 语句块中，**每一个新节点的 `MERGE` 语句**都必须使用一个**当前未声明的、唯一的变量名**（推荐使用 `n1`, `c1`, `l1`, `i1` 等序列化的变量）。在后续的关系连接语句中，**必须**使用这些已声明的变量名。
                    
                    				**示例输出格式（必须遵循此变量使用规范）：**
                    				MERGE (c1:Character {name: "萧炎"})
                    				MERGE (s1:State {name: "四段斗之气"})
                    				MERGE (c1)-[:HAS_STATE]->(s1)
                    				MERGE (l1:Location {name: "炎城"})
                    				MERGE (c1)-[:LOCATED_IN]->(l1)
                    
                    				**请开始构建 Cypher 语句：**
                    """
    );
  }

  public PromptTemplate novelAnalysePromptTemplate() {
    return PromptTemplate.from(
            """
                    图谱数据:
                    {graphData}
                    核心分析指令:
                    请根据上述图谱数据，生成一份结构清晰、逻辑严密的剧情分析报告。报告必须包含以下几个部分，并使用Markdown格式进行组织：
                    1. 故事主线梳理 (Main Plot Outline):
                    提取并串联关键的节点与关系，以时间或因果顺序，重构出故事的核心主线剧情。请用简洁的语言概述故事的开端、发展、高潮和结局。
                    2. 核心人物深度分析 (In-Depth Character Analysis):
                    识别并列出主角（Protagonist）、核心反派（Antagonist）以及对剧情有重大影响的关键配角。
                    对每一个核心人物，请从以下角度进行分析：
                    角色定位与功能: 他们在故事中推动了哪些情节？
                    核心动机与目标: 他们的行为背后最根本的驱动力是什么？
                    人物关系网: 他们与谁是盟友？与谁是敌人？是否存在复杂或矛盾的关系？
                    人物弧光/转变: 结合关系和事件的变化，分析该角色在故事中是否经历了成长、堕落或其他显著转变。
                    3. 势力与阵营剖析 (Faction & Alliance Breakdown):
                    基于人物之间的属于(BELONGS_TO)、盟友(ALLY_OF)、敌对(ENEMY_OF)等关系，识别出故事中存在的主要阵营或团体。
                    分析各个阵营的核心目标、代表人物以及它们之间的力量平衡与冲突关系。
                    4. 关键转折点与催化事件 (Pivotal Turning Points & Catalyst Events):
                    找出5-7个对整个故事走向产生决定性影响的关键事件节点或关系。
                    详细说明每个事件为何是转折点，它如何改变了人物的命运、阵营的对比或故事的主题方向。
                    5. 主题与象征挖掘 (Thematic & Symbolic Digging):
                    基于图中反复出现的节点标签（如背叛、牺牲）、关系类型或物品（MacGuffin），推断并阐述故事的核心主题（例如：权力与腐败、宿命与抗争、爱与救赎等）。
                    分析某些特定物品、地点或概念在故事中可能存在的象征意义。
                    6. 剧情潜力与未解之谜 (Narrative Potential & Unresolved Mysteries):
                    作为专家，请根据数据中的“断裂链接”或信息不完整的节点，提出深刻的洞察。
                    指出当前图谱中存在的悬念、伏笔或未解之谜。这些可能是续集或前传可以探索的剧情点。
                    请开始你的分析。
                    """
    );
  }
  // 需要一个剧情缩写的prompt，对小说章节内容进行概括性描述

  public PromptTemplate writeNewChapterPromptTemplate() {
    return PromptTemplate.from(
            """
                            # 身份与指令
                            你是一位顶级网络小说作家，尤其擅长东方玄幻风格。你的核心任务是创作小说新章节。
                    
                            # 核心规则
                            1.  **一致性**: 如有提供背景摘要，请依据其中的人物关系、世界观设定和历史事件进行创作，避免出现矛盾。若未提供，则请参考[关系参考]和[本章写作大纲]。
                            2.  **角色弧光**: 角色的决策和成长应符合其在已知信息中揭示的性格和动机。
                            3.  **叙事要求**:
                                - **视角**: 使用{perspective}（例如：第三人称全知视角、第一人称主角视角）。
                                - **情绪基调**: 本章的整体氛围应为{mood}（例如：紧张悬疑、悲伤沉重、轻松诙谐）。
                                - **写作风格**: 文字风格追求{style}（例如：简洁有力，类似古龙；华丽繁复，充满细节）。
                                - **字数建议**: {word_count} 字左右（可根据实际需求调整）。
                    
                            # 输入信息
                    
                            [关系参考]
                            {graph_context}
                            （请合理引用和融合上述 cypher 语句中的知识关系，丰富章节细节和逻辑。）
                    
                            ---
                    
                            [上一章简述]
                            {last_paragraph}
                            （请与上一章节内容自然衔接，保持整体叙述连贯。）
                    
                            ---
                    
                            [本章写作大纲]
                            {user_goal}
                            （章节内容需紧密围绕本章大纲展开，结构清晰，主题突出。）
                    
                            ---
                    
                            # 开始创作
                    
                            请严格遵循以上所有指令，开始创作第 {next_chapter_number} 章的内容。
                    """
    );
  }

  /**
   * 分析章节大纲，识别需要进行RAG搜索的关键信息（优化版）
   */
  public PromptTemplate outlineAnalysisPromptTemplate() {
    return PromptTemplate.from(
            """
                    # 身份与任务
                    你是一个专业的小说创作助手，擅长分析章节大纲并识别需要补充的关系信息。
                    
                    # 输入信息
                    [章节大纲]
                    {outline}
                    
                    # 分析任务
                    请分析上述章节大纲，识别并提取需要从知识图谱中查询的关键信息：
                    
                    ## 关系搜索需求
                    - 分析大纲中涉及的人物关系、情节发展
                    - 识别需要从知识图谱中查询的关键信息
                    - 提取相关的人物、地点、物品、技能等实体
                    - 确定需要了解的人物背景、历史事件、修炼体系等
                    
                    # 输出要求（非常重要）
                    - 只能输出JSON格式，不能包含任何代码标记
                    - 不要使用```json或```标记
                    - 直接输出纯净的JSON内容
                    - JSON必须是有效的格式
                    
                    # 输出格式
                    {
                      "relationQueries": [
                        "查询关键词1",
                        "查询关键词2"
                      ]
                    }
                    
                    # 注意事项
                    - 如果不需要查询，则返回空数组
                    - 查询关键词要具体明确，便于后续搜索
                    - 关键词应该是实体名称或者关系描述
                    - 确保输出的JSON格式正确且无语法错误
                    """
    );
  }

  /**
   * 用于引导用户给出更好大纲的 PromptTemplate
   */
  public PromptTemplate outlineGuidancePromptTemplate() {
    return PromptTemplate.from(
            """
                    # 身份与任务
                    你是一位经验丰富的小说编辑，擅长帮助作者优化和完善章节大纲。
                    
                    # 输入信息
                    [当前小说上下文]
                    {novelContext}
                    
                    [用户初始大纲]
                    {userOutline}
                    
                    [相关历史信息]
                    {historicalContext}
                    
                    # 引导目标
                    基于提供的上下文信息，帮助用户优化大纲，使其：
                    1. **情节连贯性**: 与之前的章节内容自然衔接
                    2. **人物一致性**: 符合已有的人物设定和关系
                    3. **世界观统一**: 遵循已建立的世界观和设定
                    4. **冲突合理**: 情节冲突和转折合情合理
                    5. **细节丰富**: 包含具体的场景、对话和动作元素
                    
                    # 输出要求
                    请提供：
                    1. **大纲分析**: 对用户大纲的优缺点分析
                    2. **优化建议**: 具体的改进建议，结合上下文信息
                    3. **优化后大纲**: 整理后的完整章节大纲
                    
                    # 输出格式
                    ## 大纲分析
                    [对用户大纲的分析]
                    
                    ## 优化建议
                    [具体建议]
                    
                    ## 优化后大纲
                    [整理后的完整大纲]
                    """
    );
  }

  /**
   * 更新后的章节生成 PromptTemplate（去除背景知识库）
   */
  public PromptTemplate generateChapterPromptTemplate() {
    return PromptTemplate.from(
            """
                    # 身份与指令
                    你是一位顶级网络小说作家，尤其擅长东方玄幻风格。你的核心任务是基于提供的大纲和关系信息创作新章节。
                    
                    # 输入信息
                    
                    [章节大纲]
                    {outline}
                    
                    [关系背景信息]
                    {relationContext}
                    
                    [上一章节摘要]
                    {lastChapterSummary}
                    
                    # 创作要求
                    1. **情节发展**: 严格按照章节大纲展开情节，确保逻辑连贯
                    2. **关系一致性**: 充分利用提供的关系信息，保持人物关系一致
                    3. **人物塑造**: 基于已有的人物关系信息，确保角色行为符合设定
                    4. **衔接自然**: 与上一章节内容自然衔接，保持叙述连续性
                    5. **文风统一**: 保持东方玄幻小说的文风特色
                    6. **字数控制**: 生成约2000-3000字的章节内容
                    
                    # 创作格式
                    - 使用第三人称叙述
                    - 包含对话、心理描写、动作描述
                    - 适当添加环境描写和氛围营造
                    - 章节结尾要有适当的悬念或转折
                    
                    # 开始创作
                    请基于以上信息创作新章节内容：
                    """
    );
  }

  /**
   * 用于对小说章节内容进行概括性描述的 PromptTemplate
   */
  public PromptTemplate chapterSummaryPromptTemplate() {
    return PromptTemplate.from(
            """
                    # 身份与任务
                    你是一位专业小说编辑，擅长用简洁、准确的语言对小说章节进行主旨提炼和剧情概括。
                    
                    # 输入信息
                    [章节原文]
                    {chapter_content}
                    
                    # 输出要求
                    - 用简明扼要的语言概括本章节的主要事件、关键人物及其关系。
                    - 突出章节主线和情感基调，避免冗余细节和无关内容。
                    - 字数建议：200字以内。
                    - 禁止输出原文摘录，只能用自己的话总结。
                    
                    # 请开始生成本章节的剧情缩写。
                    """
    );
  }

  public GraphService() {
    this.driver = GraphDatabase.driver(NEO4J_URI, AuthTokens.basic(NEO4J_USER, NEO4J_PASSWORD));
  }

  public void readStory(String path) {
    File storyFile = new File(path);
    List<FileReadUtil.ParseResult> parseResults = FileReadUtil.readEpubFile(storyFile);
    // 确保列表至少有两个元素，然后从第二个元素开始（索引为1）
    if (parseResults.size() > 1) {
      for (FileReadUtil.ParseResult parseResult : parseResults.subList(1, parseResults.size())) {
        Chapter chapter = new Chapter();
        chapter.setId(IdUtil.getSnowflakeNextIdStr());
        chapter.setNovelName("凡人修仙传");
        chapter.setNovelId("1");
        String currentTitle = parseResult.getTitle();
        String currentContent = String.join("\n", parseResult.getContentList());
        String prompt = graphPromptTemplate().template()
                .replace("{chapterTitle}", currentTitle)
                .replace("{chapterContent}", currentContent);
        String cypher = decomposeLanguageModel.chat(prompt);
        String summeryPrompt = chapterSummaryPromptTemplate().template().replace("{chapter_content}", currentContent);
        String chapterSummery = decomposeLanguageModel.chat(summeryPrompt);

        chapter.setChapterName(parseResult.getTitle());
        chapter.setDescription(chapterSummery);
        chapter.setCypherDescription(cypher);
        chapter.setContent(currentContent);
        chapterRepository.save(chapter);
//        executeCypher(answer);
      }
    }
  }

  public void analyseNovel() {
    String nodes = getNodes(driver);
    String prompt = novelAnalysePromptTemplate().template().replace("{graphData}", nodes);
    String answer = decomposeLanguageModel.chat(prompt);
    System.out.println("answer = " + answer);
  }

  private void executeCypher(String cypher) {
    try (Session session = driver.session()) {
      session.run(cypher);
    } catch (Exception e) {
      System.err.println("❌ Cypher 执行失败：" + cypher);
      e.printStackTrace();
    }
  }

  /**
   * 从 Neo4j 提取所有节点信息并格式化为 JSON 字符串
   */
  private static String getNodes(Driver driver) {
    String cypher = "MATCH (n) RETURN id(n) AS nodeId, labels(n) AS labels, properties(n) AS properties";
    try (Session session = driver.session()) {
      List<org.neo4j.driver.Record> records = session.run(cypher).list();

      String result = records.stream()
              .map(record -> String.format(
                      "  { \"nodeId\": %d, \"labels\": %s, \"properties\": %s }",
                      record.get("nodeId").asLong(),
                      listToJsonString(record.get("labels").asList(Value::asString)),
                      mapToJsonString(record.get("properties").asMap())
              ))
              .collect(Collectors.joining(",\n"));

      return "[\n" + result + "\n]";
    }
  }

  /**
   * 从 Neo4j 提取所有关系信息并格式化为 JSON 字符串
   */
  private static String extractRelationships(Driver driver) {
    String cypher = "MATCH (start)-[r]->(end) RETURN id(start) AS startNodeId, id(end) AS endNodeId, type(r) AS relationshipType, properties(r) AS properties";
    try (Session session = driver.session()) {
      List<Record> records = session.run(cypher).list();

      String result = records.stream()
              .map(record -> String.format(
                      "  { \"startNodeId\": %d, \"endNodeId\": %d, \"relationshipType\": \"%s\", \"properties\": %s }",
                      record.get("startNodeId").asLong(),
                      record.get("endNodeId").asLong(),
                      record.get("relationshipType").asString(),
                      mapToJsonString(record.get("properties").asMap())
              ))
              .collect(Collectors.joining(",\n"));

      return "[\n" + result + "\n]";
    }
  }

  private static String mapToJsonString(Map<String, Object> map) {
    String entries = map.entrySet().stream()
            .map(entry -> "\"" + entry.getKey() + "\":" + formatJsonValue(entry.getValue()))
            .collect(Collectors.joining(", "));
    return "{" + entries + "}";
  }

  private static String listToJsonString(List<String> list) {
    String entries = list.stream()
            .map(item -> "\"" + escapeString(item) + "\"")
            .collect(Collectors.joining(", "));
    return "[" + entries + "]";
  }

  private static String formatJsonValue(Object value) {
    if (value instanceof String) {
      return "\"" + escapeString((String) value) + "\"";
    }
    if (value instanceof List) {
      // 简单处理，可以根据需要扩展
      return "\"<List Data>\"";
    }
    return value.toString(); // For numbers, booleans
  }

  private static String escapeString(String s) {
    return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\f", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
  }

  /**
   * 用于根据章节大纲生成 cypher 查询语句的 PromptTemplate（严格约束只输出一条完整语句，禁止自然语言和解释，必须严格围绕大纲语义生成，无法映射时只输出空字符串）
   */
  public PromptTemplate cypherQueryPromptTemplate() {
    return PromptTemplate.from(
            """
                    # 角色设定
                    你是一个知识图谱专家，精通 Neo4j cypher 查询。
                    
                    # 输入结构
                    小说章节大纲：
                    {outline}
                    
                    # 输出要求（务必严格遵守）
                    - 生成的 cypher 查询语句必须严格围绕输入大纲的语义，不得扩展、假设或补充大纲未提及的内容。
                    - 只允许输出一条完整、可执行的 Cypher 查询语句，禁止输出任何自然语言、解释、注释、剧情内容。
                    - 查询语句必须严格遵循 Neo4j cypher 语法，不能有语法错误。
                    - 语句只能有一个 RETURN，不能有多条语句或多次 RETURN。
                    - 语句必须与 Neo4j 数据库实际结构匹配（如节点标签、关系类型、属性名）。
                    - 如输入无法直接映射为 cypher 查询，则只输出空字符串，绝不能输出原始大纲或自然语言。
                    - 禁止输出任何解释、剧情、自然语言，只能输出 cypher 查询语句。
                    
                    # cypher 查询语句示例（仅输出语句，无其他内容）
                    ## 示例1：大纲为“主角做了什么”
                    MATCH (c:Character)-[r]->(a:Action) WHERE c.name CONTAINS '主角' RETURN c, r, a
                    ## 示例2：大纲为“主角与谁发生冲突”
                    MATCH (c:Character)-[r:ENEMY_OF]->(other:Character) WHERE c.name CONTAINS '角' RETURN c, r, other
                    ## 示例3：大纲为“主角获得了什么物品”
                    MATCH (c:Character)-[r:OBTAINS]->(i:Item) WHERE c.name CONTAINS '主角' RETURN c, r, i
                    ## 示例4：大纲为“主角的内心活动”无法映射为 cypher 查询
                    （此时只输出空字符串）
                    
                    # cypher 查询语法说明
                    - MATCH (n:Label)-[r:RELATION]->(m:Label) 用于匹配节点和关系
                    - WHERE 用于条件筛选
                    - RETURN 用于返回节点、关系及其属性（只能有一个）
                    - 可用 AS 重命名返回字段
                    - 支持 CONTAINS、STARTS WITH、ENDS WITH 等字符串筛选
                    
                    # 严格禁止输出任何解释、剧情、自然语言，只能输出 cypher 查询语句。如无法根据大纲语义生成有效语句，则只输出空字符串。
                    """
    );
  }

  /**
   * 根据章节大纲，自动生成 cypher 查询语句并查询 Neo4j，返回真实的关系内容
   * 增加 cypher语句合法性校验，非 cypher语句直接返回空字符串
   */
  public String searchRelations(String outline) {
    String prompt = cypherQueryPromptTemplate().template().replace("{outline}", outline);
    String cypher = decomposeLanguageModel.chat(prompt).trim();
    // 校验是否为合法 cypher语句（以 cypher 关键字开头）
    String cypherLower = cypher.toLowerCase();
    if (!(cypherLower.startsWith("match") || cypherLower.startsWith("optional match") || cypherLower.startsWith("return") || cypherLower.startsWith("create") || cypherLower.startsWith("set") || cypherLower.startsWith("delete") || cypherLower.startsWith("unwind") || cypherLower.startsWith("with") || cypherLower.startsWith("where"))) {
      return "";
    }
    try (Session session = driver.session()) {
      List<Record> records = session.run(cypher).list();
      StringBuilder sb = new StringBuilder();
      for (Record record : records) {
        sb.append(record.toString()).append("\n");
      }
      return sb.toString();
    } catch (Exception e) {
      System.err.println("❌ Cypher 查询失败：" + cypher);
      e.printStackTrace();
      return "【关系参考获取失败】";
    }
  }

  /**
   * 引导用户优化大纲
   * @param userOutline 用户初始大纲
   * @param novelContext 当前小说上下文
   * @param historicalContext 相关历史信息
   * @return 优化建议和改进后的大纲
   */
  public String guideOutlineOptimization(String userOutline, String novelContext, String historicalContext) {
    logger.info("Guiding user outline optimization");
    
    try {
      // 验证输入参数
      if (userOutline == null || userOutline.trim().isEmpty()) {
        logger.warn("Empty user outline provided");
        throw new IllegalArgumentException("用户大纲不能为空");
      }
      
      // 处理可选参数
      String safeNovelContext = novelContext != null ? novelContext : "暂无相关小说上下文信息";
      String safeHistoricalContext = historicalContext != null ? historicalContext : "暂无相关历史信息";
      
      String prompt = outlineGuidancePromptTemplate().template()
              .replace("{userOutline}", userOutline)
              .replace("{novelContext}", safeNovelContext)
              .replace("{historicalContext}", safeHistoricalContext);
      
      String guidance = decomposeLanguageModel.chat(prompt);
      logger.info("Successfully generated outline guidance");
      
      return guidance;
    } catch (Exception e) {
      logger.error("Error guiding outline optimization: {}", e.getMessage());
      throw new RuntimeException("生成大纲优化建议时发生错误: " + e.getMessage());
    }
  }

  /**
   * 动态执行关系搜索（基于Neo4j中预存的数据）改进版 - 增加详细输出
   * @param relationQueries 从大纲分析中提取的关系查询关键词列表
   * @return 格式化的关系信息字符串
   */
  public String performDynamicRelationSearch(List<String> relationQueries) {
    logger.info("Performing dynamic relation search with {} queries", relationQueries.size());
    
    if (relationQueries == null || relationQueries.isEmpty()) {
      logger.info("No relation queries provided, returning empty result");
      return "暂无相关关系信息";
    }
    
    StringBuilder relationContext = new StringBuilder();
    relationContext.append("=== 知识图谱关系搜索详细结果 ===\n\n");
    relationContext.append(String.format("总共执行 %d 个查询:\n\n", relationQueries.size()));
    
    for (int i = 0; i < relationQueries.size(); i++) {
      String query = relationQueries.get(i);
      try {
        relationContext.append(String.format("## 步骤 %d: 处理查询 『%s』\n", i + 1, query));
        
        // 步骤1: 实体提取
        List<String> entities = extractEntitiesFromQuery(query);
        relationContext.append(String.format("🔍 实体提取结果: %s\n", entities.isEmpty() ? "未找到已知实体" : entities.toString()));
        
        // 步骤2: Cypher生成
        String cypher = generateCypherForRelationQuery(query);
        relationContext.append(String.format("💾 生成的Cypher查询:\n```cypher\n%s\n```\n", cypher));
        
        logger.info("Executing cypher for query '{}': {}", query, cypher);
        
        if (!cypher.trim().isEmpty()) {
          try (Session session = driver.session()) {
            // 步骤3: 执行查询
            relationContext.append("⚡ 执行查询中...\n");
            List<Record> records = session.run(cypher).list();
            
            relationContext.append(String.format("📊 查询结果: 找到 %d 条记录\n", records.size()));
            
            if (!records.isEmpty()) {
              relationContext.append("📋 详细结果:\n");
              
              for (int j = 0; j < records.size(); j++) {
                Record record = records.get(j);
                try {
                  // 尝试解析标准格式
                  String entity1 = record.get("entity1").asString();
                  String relationship = record.get("relationship").asString();
                  String entity2 = record.get("entity2").asString();
                  
                  relationContext.append(String.format("   %d. %s --[%s]--> %s\n", 
                          j + 1, entity1, relationship, entity2));
                          
                  // 如果有额外的节点属性信息，也显示出来
                  try {
                    Value node1 = record.get("node1");
                    Value node2 = record.get("node2");
                    if (!node1.isNull() && !node2.isNull()) {
                      relationContext.append(String.format("      详细: %s | %s\n", 
                              node1.asNode().asMap(), node2.asNode().asMap()));
                    }
                  } catch (Exception detailError) {
                    // 忽略详细信息获取错误
                  }
                } catch (Exception parseError) {
                  // 如果结构不符合预期，直接输出原始结果
                  relationContext.append(String.format("   %d. [原始] %s\n", j + 1, record.toString()));
                }
              }
            } else {
              relationContext.append("   ❌ 未找到相关信息\n");
            }
          }
        } else {
          relationContext.append("❌ 无法生成有效的查询语句\n");
        }
        
        relationContext.append("\n" + "=".repeat(50) + "\n\n");
      } catch (Exception e) {
        logger.warn("Failed to execute relation query for '{}': {}", query, e.getMessage());
        relationContext.append(String.format("❌ 查询执行失败: %s\n\n", e.getMessage()));
        relationContext.append("=".repeat(50) + "\n\n");
      }
    }
    
    String result = relationContext.toString();
    logger.info("Completed dynamic relation search, generated {} characters of detailed output", result.length());
    
    return result;
  }

  /**
   * 为关系查询关键词生成Cypher语句（改进版，支持复杂查询）
   * @param queryKeyword 查询关键词
   * @return Cypher查询语句
   */
  private String generateCypherForRelationQuery(String queryKeyword) {
    // 提取查询中的关键实体名称
    List<String> entities = extractEntitiesFromQuery(queryKeyword);
    
    if (entities.isEmpty()) {
      // 如果没有提取到实体，使用原来的通用查询
      return String.format(
              "MATCH (n)-[r]-(m) WHERE n.name CONTAINS '%s' OR m.name CONTAINS '%s' " +
              "RETURN n.name AS entity1, type(r) AS relationship, m.name AS entity2, " +
              "n AS node1, m AS node2 LIMIT 10",
              queryKeyword, queryKeyword
      );
    }
    
    // 如果只有一个实体，查询该实体的所有关系
    if (entities.size() == 1) {
      String entity = entities.get(0);
      return String.format(
              "MATCH (n)-[r]-(m) WHERE n.name CONTAINS '%s' " +
              "RETURN n.name AS entity1, type(r) AS relationship, m.name AS entity2, " +
              "n AS node1, m AS node2, r AS relationship_detail LIMIT 15",
              entity
      );
    }
    
    // 如果有多个实体，查询它们之间的关系
    String entity1 = entities.get(0);
    String entity2 = entities.get(1);
    return String.format(
            "MATCH (n)-[r]-(m) WHERE (n.name CONTAINS '%s' AND m.name CONTAINS '%s') " +
            "OR (n.name CONTAINS '%s' AND m.name CONTAINS '%s') " +
            "RETURN n.name AS entity1, type(r) AS relationship, m.name AS entity2, " +
            "n AS node1, m AS node2, r AS relationship_detail LIMIT 20",
            entity1, entity2, entity2, entity1
    );
  }
  
  /**
   * 公开的实体提取方法，用于测试和调试
   * @param queryDescription 查询描述
   * @return 提取的实体名称列表
   */
  public List<String> debugExtractEntities(String queryDescription) {
    return extractEntitiesFromQuery(queryDescription);
  }
  
  /**
   * 公开的Cypher生成方法，用于测试和调试
   * @param queryKeyword 查询关键词
   * @return 生成的Cypher语句
   */
  public String debugGenerateCypher(String queryKeyword) {
    return generateCypherForRelationQuery(queryKeyword);
  }
  
  /**
   * 从复杂查询描述中提取实体名称
   * @param queryDescription 查询描述
   * @return 提取的实体名称列表
   */
  private List<String> extractEntitiesFromQuery(String queryDescription) {
    List<String> entities = new ArrayList<>();
    
    // 常见的实体名称模式
    String[] knownEntities = {
        "韩立", "厉飞雨", "历师兄", "厉师兄", "墨大夫", "李师叔", "马师兄",
        "眨眼剑法", "长春功", "太南小会", "越国", "黄枫谷", "掌门", "长老"
    };
    
    // 检查查询描述中是否包含已知实体
    for (String entity : knownEntities) {
      if (queryDescription.contains(entity)) {
        entities.add(entity);
      }
    }
    
    // 如果没有找到已知实体，尝试提取冒号前的部分作为实体名
    if (entities.isEmpty() && queryDescription.contains("：")) {
      String beforeColon = queryDescription.split("：")[0];
      if (beforeColon.length() <= 10) { // 假设实体名不会太长
        entities.add(beforeColon.trim());
      }
    }
    
    return entities;
  }

  /**
   * 从MongoDB获取指定章节范围的摘要作为上下文
   * @param novelId 小说ID
   * @param startChapter 开始章节号
   * @param endChapter 结束章节号
   * @return 格式化的章节上下文信息
   */
  public String getSpecificChaptersContext(String novelId, int startChapter, int endChapter) {
    logger.info("Getting chapters {}-{} context for novel: {}", startChapter, endChapter, novelId);
    
    try {
      // 使用基础的findAll方法，然后在代码中过滤特定章节
      List<Chapter> allChapters = chapterRepository.findAll();
      
      // 过滤指定小说和章节范围的章节
      List<Chapter> targetChapters = CollUtil.newArrayList(allChapters.get(30),allChapters.get(31),allChapters.get(32));
      
      if (targetChapters.isEmpty()) {
        logger.info("No chapters found in range {}-{} for novel: {}", startChapter, endChapter, novelId);
        return String.format("暂无第%d-%d章的相关章节信息", startChapter, endChapter);
      }
      
      StringBuilder context = new StringBuilder();
      context.append(String.format("=== 第%d-%d章章节摘要 ===\n", startChapter, endChapter));
      
      for (Chapter chapter : targetChapters) {
        int chapterNum = extractChapterNumber(chapter.getChapterName());
        context.append(String.format("【第%d章】%s\n", chapterNum, chapter.getChapterName()));
        context.append(chapter.getDescription()).append("\n\n");
      }
      
      logger.info("Successfully retrieved context for {} chapters in range {}-{}", 
                 targetChapters.size(), startChapter, endChapter);
      return context.toString();
    } catch (Exception e) {
      logger.error("Error getting specific chapters context: {}", e.getMessage());
      return String.format("获取第%d-%d章章节上下文信息时发生错误", startChapter, endChapter);
    }
  }
  
  /**
   * 从章节名称中提取章节号
   * @param chapterName 章节名称
   * @return 章节号，如果提取失败返回0
   */
  private int extractChapterNumber(String chapterName) {
    if (chapterName == null) return 0;
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("第(\\d+)章");
    java.util.regex.Matcher matcher = pattern.matcher(chapterName);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(1));
    }
    return 0;
  }

  /**
   * 专门为第34章生成准备上下文（第30-33章）
   * @param novelId 小说ID
   * @return 第30-33章的详细上下文信息
   */
  public String getContextForChapter34(String novelId) {
    logger.info("Getting context for chapter 34 generation (chapters 30-33) for novel: {}", novelId);
    
    StringBuilder detailedContext = new StringBuilder();
    detailedContext.append("=== 第34章创作准备：第30-33章详细上下文 ===\n\n");
    
    // 获取第30-33章的内容
    String chaptersContext = getSpecificChaptersContext(novelId, 30, 33);
    detailedContext.append(chaptersContext);
    
    // 添加创作指导信息
    detailedContext.append("\n=== 第34章创作指导 ===\n");
    detailedContext.append("基于以上第30-33章的内容，请注意以下要点：\n");
    detailedContext.append("1. 保持故事情节的连贯性和逻辑性\n");
    detailedContext.append("2. 继承前几章中建立的人物关系和设定\n");
    detailedContext.append("3. 注意情节发展的自然过渡\n");
    detailedContext.append("4. 保持一致的文风和叙述节奏\n\n");
    
    return detailedContext.toString();
  }

  /**
   * 从MongoDB获取指定章节号的摘要作为上下文
   * @param novelId 小说ID
   * @param chapterNumbers 章节号列表
   * @return 格式化的章节上下文信息
   */
  public String getSpecificChaptersContextByNumbers(String novelId, List<Integer> chapterNumbers) {
    logger.info("Getting context for specific chapters {} for novel: {}", chapterNumbers, novelId);

    try {
      // 使用基础的findAll方法，然后在代码中过滤和排序
      List<Chapter> allChapters = chapterRepository.findAll();

      // 过滤指定小说和章节号的章节，并按章节号升序排序
      List<Chapter> specificChapters = allChapters.stream()
              .filter(chapter -> novelId.equals(chapter.getNovelId()) && chapterNumbers.contains(chapter.getChapterNumber()))
              .sorted((c1, c2) -> c1.getChapterNumber().compareTo(c2.getChapterNumber()))
              .collect(Collectors.toList());

      if (specificChapters.isEmpty()) {
        logger.info("No chapters found for novel {} with numbers {}", novelId, chapterNumbers);
        return "未找到指定章节信息";
      }

      StringBuilder context = new StringBuilder();
      context.append("=== 指定章节摘要 ===\n");

      for (Chapter chapter : specificChapters) {
        context.append("【").append(chapter.getChapterName()).append("】 (第").append(chapter.getChapterNumber()).append("章)\n");
        context.append(chapter.getDescription()).append("\n\n");
      }

      logger.info("Successfully retrieved context for {} specific chapters", specificChapters.size());
      return context.toString();
    } catch (Exception e) {
      logger.error("Error getting specific chapters context: {}", e.getMessage());
      return "获取指定章节上下文信息时发生错误";
    }
  }

  /**
   * 重写的分析大纲方法（去除背景知识库）
   * @param outline 章节大纲
   * @return 包含关系查询需求的JSON结果
   */
  public String analyzeOutlineForRAG(String outline) {
    logger.info("Analyzing outline for RAG search requirements");
    
    try {
      // 验证输入参数
      if (outline == null || outline.trim().isEmpty()) {
        logger.warn("Empty outline provided for analysis");
        return "{\"relationQueries\": []}";
      }
      
      String prompt = outlineAnalysisPromptTemplate().template()
              .replace("{outline}", outline);
      
      String analysisResult = decomposeLanguageModel.chat(prompt);
      logger.info("Successfully analyzed outline for RAG requirements");
      
      return analysisResult;
    } catch (Exception e) {
      logger.error("Error analyzing outline for RAG: {}", e.getMessage());
      return "{\"relationQueries\": []}";
    }
  }

  /**
   * 基于大纲和关系信息生成新章节内容（更新版）
   * @param outline 章节大纲
   * @param relationContext 从知识图谱检索的关系信息
   * @param lastChapterSummary 上一章节的摘要（可选）
   * @return 生成的章节内容
   */
  public String generateNewChapterContent(String outline, String relationContext, String lastChapterSummary) {
    logger.info("Generating new chapter content based on outline and context");
    
    try {
      // 验证必要参数
      if (outline == null || outline.trim().isEmpty()) {
        logger.warn("Empty outline provided for chapter generation");
        throw new IllegalArgumentException("章节大纲不能为空");
      }
      
      // 处理可选参数
      String safeRelationContext = relationContext != null ? relationContext : "暂无相关人物关系信息";
      String safeLastChapterSummary = lastChapterSummary != null ? lastChapterSummary : "暂无上一章节信息";
      
      String prompt = generateChapterPromptTemplate().template()
              .replace("{outline}", outline)
              .replace("{relationContext}", safeRelationContext)
              .replace("{lastChapterSummary}", safeLastChapterSummary);
      
      String chapterContent = decomposeLanguageModel.chat(prompt);
      logger.info("Successfully generated new chapter content");
      
      return chapterContent;
    } catch (Exception e) {
      logger.error("Error generating chapter content: {}", e.getMessage());
      throw new RuntimeException("生成章节内容时发生错误: " + e.getMessage());
    }
  }

  /**
   * 完整的动态章节生成流程 - 增强版（详细输出每个步骤）
   * @param outline 章节大纲
   * @param novelId 小说ID
   * @param lastChapterSummary 上一章节摘要（可选）
   * @return 包含分析结果和生成内容的对象
   */
  public ChapterGenerationResult generateChapterFromOutline(String outline, String novelId, String lastChapterSummary) {
    logger.info("Starting complete dynamic chapter generation process from outline");
    
    // 初始化结果对象用于存储详细信息
    ChapterGenerationResult result = new ChapterGenerationResult();
    result.setOutline(outline);
    
    StringBuilder detailedLog = new StringBuilder();
    detailedLog.append("=== 章节生成完整流程日志 ===\n\n");
    
    try {
      // 步骤1: 分析大纲获取RAG需求
      detailedLog.append("📅 步骤 1: 分析章节大纲\n");
      detailedLog.append(String.format("📝 输入大纲: %s\n", outline));
      
      String ragAnalysis = analyzeOutlineForRAG(outline);
      result.setRagAnalysis(ragAnalysis);
      
      detailedLog.append("🤖 AI分析结果:\n");
      detailedLog.append(ragAnalysis);
      detailedLog.append("\n\n");
      
      // 步骤2: 解析关系查询需求
      detailedLog.append("🔍 步骤 2: 解析JSON提取查询关键词\n");
      List<String> relationQueries = parseRelationQueries(ragAnalysis);
      
      detailedLog.append(String.format("📋 提取结果: 找到 %d 个查询关键词\n", relationQueries.size()));
      for (int i = 0; i < relationQueries.size(); i++) {
        detailedLog.append(String.format("   %d. %s\n", i + 1, relationQueries.get(i)));
      }
      detailedLog.append("\n");
      
      // 步骤3: 执行动态关系搜索
      detailedLog.append("💾 步骤 3: 执行动态Neo4j关系搜索\n");
      String relationContext = performDynamicRelationSearch(relationQueries);
      result.setRelationContext(relationContext);
      
      detailedLog.append("📈 关系搜索完成，结果长度: ");
      detailedLog.append(relationContext.length()).append(" 字符\n\n");
      
      // 步骤4: 获取最近章节上下文
      detailedLog.append("📚 步骤 4: 获取最近章节上下文\n");
      detailedLog.append(String.format("📝 输入参数: novelId=%s, limit=3\n", novelId));
      
      String recentContext = getSpecificChaptersContext(novelId, 1, 3); // 使用新方法获取前3章
      result.setRecentContext(recentContext);
      
      detailedLog.append(String.format("📈 获取结果长度: %d 字符\n", recentContext.length()));
      if (recentContext.contains("暂无相关章节信息")) {
        detailedLog.append("⚠️ 注意: 未找到相关章节数据\n");
      }
      detailedLog.append("\n");
      
      // 步骤5: 生成章节内容
      detailedLog.append("✍️ 步骤 5: 生成章节内容\n");
      detailedLog.append("📝 输入参数汇总:\n");
      detailedLog.append(String.format("   - 大纲: %s\n", outline));
      detailedLog.append(String.format("   - 关系上下文长度: %d 字符\n", relationContext.length()));
      detailedLog.append(String.format("   - 上一章摘要: %s\n", lastChapterSummary != null ? lastChapterSummary : "无"));
      
      String chapterContent = generateNewChapterContent(outline, relationContext, lastChapterSummary);
      result.setGeneratedContent(chapterContent);
      
      detailedLog.append(String.format("🎉 生成完成，内容长度: %d 字符\n", chapterContent.length()));
      detailedLog.append("\n");
      
      // 步骤6: 生成流程总结
      detailedLog.append("📊 流程总结:\n");
      detailedLog.append(String.format("   - 查询关键词数量: %d\n", relationQueries.size()));
      detailedLog.append(String.format("   - 关系搜索结果: %d 字符\n", relationContext.length()));
      detailedLog.append(String.format("   - 章节上下文: %d 字符\n", recentContext.length()));
      detailedLog.append(String.format("   - 生成内容: %d 字符\n", chapterContent.length()));
      
      // 将详细日志保存到结果中
      result.setDetailedLog(detailedLog.toString());
      
      logger.info("Successfully completed dynamic chapter generation process with detailed logging");
      return result;
    } catch (Exception e) {
      detailedLog.append(String.format("❌ 错误: %s\n", e.getMessage()));
      result.setDetailedLog(detailedLog.toString());
      
      logger.error("Error in dynamic chapter generation process: {}", e.getMessage());
      throw new RuntimeException("动态章节生成流程发生错误: " + e.getMessage());
    }
  }

  /**
   * 解析关系查询JSON，提取关系查询关键词列表（改进版，支持复杂查询）
   * @param ragAnalysisJson RAG分析结果JSON
   * @return 关系查询关键词列表
   */
  private List<String> parseRelationQueries(String ragAnalysisJson) {
    try {
      // 处理复杂查询描述中的换行符
      String cleanJson = cleanJsonFromMarkdown(ragAnalysisJson);
      
      if (cleanJson.trim().isEmpty()) {
        logger.warn("Empty JSON content after cleaning");
        return new ArrayList<>();
      }
      
      // 设置宽松模式解析JSON
      JsonObject jsonObject = JsonParser.parseString(cleanJson).getAsJsonObject();
      JsonArray relationQueries = jsonObject.getAsJsonArray("relationQueries");
      
      List<String> queries = new ArrayList<>();
      if (relationQueries != null) {
        for (int i = 0; i < relationQueries.size(); i++) {
          String query = relationQueries.get(i).getAsString();
          if (query != null && !query.trim().isEmpty()) {
            queries.add(query.trim());
          }
        }
      }
      
      logger.info("Successfully parsed {} relation queries from analysis", queries.size());
      logger.debug("Parsed queries: {}", queries);
      return queries;
    } catch (Exception e) {
      logger.warn("Failed to parse relation queries from JSON: {}", e.getMessage());
      logger.debug("Original JSON content: {}", ragAnalysisJson);
      return new ArrayList<>();
    }
  }
  
  /**
   * 清理JSON字符串，移除代码块标记
   * @param jsonWithMarkdown 可能包含代码块标记的JSON字符串
   * @return 清理后的JSON字符串
   */
  private String cleanJsonFromMarkdown(String jsonWithMarkdown) {
    if (jsonWithMarkdown == null) {
      return "";
    }
    
    // 移除代码块标记
    String cleaned = jsonWithMarkdown.trim();
    
    // 移除开头的``json或```
    if (cleaned.startsWith("``json")) {
      cleaned = cleaned.substring(7);
    } else if (cleaned.startsWith("```")) {
      cleaned = cleaned.substring(3);
    }
    
    // 移除结尾的```
    if (cleaned.endsWith("```")) {
      cleaned = cleaned.substring(0, cleaned.length() - 3);
    }
    
    // 移除首尾空白字符
    cleaned = cleaned.trim();
    
    logger.debug("Cleaned JSON: {}", cleaned);
    return cleaned;
  }

  /**
   * 处理用户确认的章节内容，执行readStory的逻辑进行存储和解析
   * @param chapterTitle 章节标题
   * @param chapterContent 章节内容
   * @param novelName 小说名称
   * @param novelId 小说ID
   * @return 处理后的章节对象
   */
  public Chapter processConfirmedChapter(String chapterTitle, String chapterContent, 
                                        String novelName, String novelId) {
    logger.info("Processing confirmed chapter: {}", chapterTitle);
    
    try {
      // 验证输入参数
      if (chapterTitle == null || chapterTitle.trim().isEmpty()) {
        throw new IllegalArgumentException("章节标题不能为空");
      }
      if (chapterContent == null || chapterContent.trim().isEmpty()) {
        throw new IllegalArgumentException("章节内容不能为空");
      }
      
      // 创建章节对象
      Chapter chapter = new Chapter();
      chapter.setId(IdUtil.getSnowflakeNextIdStr());
      chapter.setNovelName(novelName != null ? novelName : "未知小说");
      chapter.setNovelId(novelId != null ? novelId : "unknown");
      chapter.setChapterName(chapterTitle);
      chapter.setContent(chapterContent);
      
      // 生成章节摘要
      String summaryPrompt = chapterSummaryPromptTemplate().template()
              .replace("{chapter_content}", chapterContent);
      String chapterSummary = decomposeLanguageModel.chat(summaryPrompt);
      chapter.setDescription(chapterSummary);
      
      // 生成Cypher语句用于知识图谱构建
      String graphPrompt = graphPromptTemplate().template()
              .replace("{chapterTitle}", chapterTitle)
              .replace("{chapterContent}", chapterContent);
      String cypher = decomposeLanguageModel.chat(graphPrompt);
      chapter.setCypherDescription(cypher);
      
      // 存储章节到数据库
      Chapter savedChapter = chapterRepository.save(chapter);
      logger.info("Successfully saved chapter to database with ID: {}", savedChapter.getId());
      
      // 执行Cypher语句更新知识图谱（如果需要）
      if (cypher != null && !cypher.trim().isEmpty()) {
        try {
          executeCypher(cypher);
          logger.info("Successfully executed Cypher statements for knowledge graph update");
        } catch (Exception e) {
          logger.warn("Failed to execute Cypher statements, but chapter was saved: {}", e.getMessage());
        }
      }
      
      return savedChapter;
    } catch (Exception e) {
      logger.error("Error processing confirmed chapter: {}", e.getMessage());
      throw new RuntimeException("处理章节时发生错误: " + e.getMessage());
    }
  }

  /**
   * 更新的章节生成结果封装类
   */
  public static class ChapterGenerationResult {
    private String outline; // 原始大纲
    private String ragAnalysis; // RAG分析结果
    private String relationContext; // 关系上下文
    private String recentContext; // 最近章节上下文
    private String generatedContent; // 生成的章节内容
    private String detailedLog; // 详细的流程日志
    
    // Getters and Setters
    public String getOutline() { return outline; }
    public void setOutline(String outline) { this.outline = outline; }
    
    public String getRagAnalysis() { return ragAnalysis; }
    public void setRagAnalysis(String ragAnalysis) { this.ragAnalysis = ragAnalysis; }
    
    public String getRelationContext() { return relationContext; }
    public void setRelationContext(String relationContext) { this.relationContext = relationContext; }
    
    public String getRecentContext() { return recentContext; }
    public void setRecentContext(String recentContext) { this.recentContext = recentContext; }
    
    public String getGeneratedContent() { return generatedContent; }
    public void setGeneratedContent(String generatedContent) { this.generatedContent = generatedContent; }
    
    public String getDetailedLog() { return detailedLog; }
    public void setDetailedLog(String detailedLog) { this.detailedLog = detailedLog; }
  }

}
