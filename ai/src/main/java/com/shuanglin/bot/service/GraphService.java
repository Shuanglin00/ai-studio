package com.shuanglin.bot.service;

import cn.hutool.core.util.IdUtil;
import com.shuanglin.bot.utils.FileReadUtil;
import com.shuanglin.dao.novel.store.Chapter;
import com.shuanglin.dao.novel.store.ChapterRepository;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaChatModel;
import jakarta.annotation.Resource;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GraphService {
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

}
