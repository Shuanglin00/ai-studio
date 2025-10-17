# EPUB前40章知识图谱构建功能 - 使用文档

## 功能概述

本功能实现了读取EPUB文件（斗破苍穹）前40章节，并构建知识图谱的完整流程。核心特性包括：

- ✅ **章节限制读取**：支持限制读取指定数量的章节（如前40章）
- ✅ **MongoDB持久化**：章节内容和LLM生成的Cypher语句存储到MongoDB
- ✅ **数据隔离**：通过元数据标识实现测试数据与生产数据隔离
- ✅ **Cypher重放**：支持从MongoDB重新执行Cypher到Neo4j
- ✅ **数据清理**：支持快速清理测试数据
- ✅ **统计查询**：提供多维度的数据统计查询

## 核心组件

### 1. 数据模型扩展

#### ArticlesEntity（扩展字段）

```java
// 原有字段
private String id;              // 文档唯一标识
private String title;           // 章节标题
private String content;         // 章节内容
private String tags;            // 标签
private String createTime;      // 创建时间

// 新增字段
private String bookUuid;              // 书籍唯一标识
private Integer chapterIndex;         // 章节序号（1-N）
private String cypherStatements;      // LLM生成的Cypher语句
private String cypherExecuteStatus;   // Cypher执行状态（SUCCESS/FAILED/PENDING）
private String cypherExecuteTime;     // Cypher执行时间
private String cypherErrorMessage;    // Cypher执行错误信息
private String processStatus;         // 章节处理状态（PENDING/PROCESSING/COMPLETED/FAILED）
private Integer paragraphCount;       // 段落总数
private String dataSource;            // 数据源标识（用于数据隔离）
private String metadata;              // 扩展元数据（JSON字符串）
```

### 2. 核心服务

#### GraphService（扩展方法）

| 方法名 | 功能 | 参数 | 返回值 |
|--------|------|------|--------|
| `readStoryWithLimit()` | 限制章节数读取并构建图谱 | path, chapterLimit, metadata | ProcessReport |
| `injectMetadata()` | 向Cypher注入隔离元数据 | cypher, metadata, mongoDocId | String |
| `replayCypherFromMongo()` | 从MongoDB重放Cypher到Neo4j | bookUuid, chapterIndex | Boolean |
| `cleanupTestData()` | 清理测试数据 | dataSource | CleanupReport |
| `queryTestDataStats()` | 查询测试数据统计 | dataSource | String |

#### ChapterStorageService（新增服务）

| 方法名 | 功能 | 参数 | 返回值 |
|--------|------|------|--------|
| `saveChapterWithCypher()` | 保存章节和Cypher | entity | ArticlesEntity |
| `updateCypherContent()` | 更新Cypher内容 | entity | ArticlesEntity |
| `updateCypherExecuteStatus()` | 更新执行状态 | docId, status, errorMsg, time | Boolean |
| `queryChaptersByBook()` | 查询书籍所有章节 | bookUuid | List<ArticlesEntity> |
| `queryChapterByIndex()` | 查询指定章节 | bookUuid, chapterIndex | ArticlesEntity |
| `queryCypherByChapter()` | 获取Cypher语句 | bookUuid, chapterIndex | String |
| `queryFailedChapters()` | 查询失败章节 | bookUuid | List<ArticlesEntity> |
| `deleteBookData()` | 删除书籍数据 | bookUuid | Long |
| `getBookStatistics()` | 获取统计信息 | bookUuid | BookStats |

### 3. 数据模型

#### IsolationMetadata（数据隔离元数据）

```java
private String dataSource;        // 数据源标识（如：test_epub_40）
private String bookName;          // 书籍名称（如：斗破苍穹）
private String bookUuid;          // 书籍唯一标识（UUID）
private Integer chapterLimit;     // 章节数量限制
private String createdBy;         // 创建者标识
private String createdAt;         // 创建时间
private Map<String, String> tags; // 自定义标签
```

#### ProcessReport（处理报告）

```java
private String bookUuid;           // 书籍UUID
private String bookName;           // 书籍名称
private Integer totalChapters;     // 处理章节总数
private Integer successChapters;   // 成功章节数
private Integer failedChapters;    // 失败章节数
private Integer skippedChapters;   // 跳过章节数
private Long totalDuration;        // 总耗时（毫秒）
private Long avgChapterDuration;   // 平均每章耗时（毫秒）
```

#### CleanupReport（清理报告）

```java
private String dataSource;              // 数据源标识
private Integer neo4jNodesDeleted;      // Neo4j删除节点数
private Integer neo4jRelationsDeleted;  // Neo4j删除关系数
private Long mongoDocsDeleted;          // MongoDB删除文档数
private Long cleanupDuration;           // 清理耗时（毫秒）
private String cleanupTime;             // 清理时间
```

## 使用指南

### 场景1：读取EPUB前40章并构建知识图谱

```java
@Resource
private GraphService graphService;

@Test
public void buildKnowledgeGraph() {
    // 步骤1: 准备元数据
    String bookUuid = UUID.randomUUID().toString();
    IsolationMetadata metadata = new IsolationMetadata();
    metadata.setDataSource("test_epub_40");
    metadata.setBookName("斗破苍穹");
    metadata.setBookUuid(bookUuid);
    metadata.setChapterLimit(40);
    metadata.setCreatedBy("test_user");
    
    // 步骤2: 验证元数据
    metadata.validate();
    
    // 步骤3: 读取EPUB并构建图谱
    String epubPath = "C:\\Users\\Shuan\\Downloads\\斗破苍穹-天蚕土豆.epub";
    ProcessReport report = graphService.readStoryWithLimit(epubPath, 40, metadata);
    
    // 步骤4: 查看处理报告
    System.out.println(report);
}
```

**执行流程：**

1. **读取EPUB**：FileReadUtil解析EPUB文件
2. **章节限制**：截取前40章
3. **遍历章节**：对每个章节执行以下步骤：
   - a. 聚合段落为完整文本
   - b. 保存章节内容到MongoDB（状态：PENDING）
   - c. 调用LLM生成Cypher语句
   - d. 更新MongoDB中的cypherStatements字段
   - e. 验证Cypher语句
   - f. 注入元数据（dataSource, bookName, bookUuid, mongoDocId）
   - g. 执行Cypher到Neo4j
   - h. 更新MongoDB中的执行状态（SUCCESS/FAILED）
4. **返回报告**：包含成功/失败统计、耗时等信息

**预期输出：**

```
📚 开始处理《斗破苍穹》前 40 个章节...
✅ 已处理章节 1/40: 第一章 落魄天才 (耗时: 5234ms)
✅ 已处理章节 2/40: 第二章 异火 (耗时: 4987ms)
...
✅ 已处理章节 40/40: 第四十章 战斗 (耗时: 5123ms)

📊 知识图谱构建完成！共处理 40 个章节

📊 知识图谱构建报告
========================================
书籍信息: 斗破苍穹 (uuid-xxx-xxx)
处理章节: 40 章
成功: 38 | 失败: 2 | 跳过: 0
总耗时: 215.6 秒
平均每章: 5.4 秒
========================================
```

### 场景2：查询章节的Cypher语句

```java
@Resource
private ChapterStorageService chapterStorageService;

@Test
public void queryCypher() {
    String bookUuid = "your-book-uuid";
    Integer chapterIndex = 1;
    
    // 方法1: 仅获取Cypher语句
    String cypher = chapterStorageService.queryCypherByChapter(bookUuid, chapterIndex);
    System.out.println("📝 Cypher语句:\n" + cypher);
    
    // 方法2: 获取完整章节信息
    ArticlesEntity chapter = chapterStorageService.queryChapterByIndex(bookUuid, chapterIndex);
    System.out.println("章节标题: " + chapter.getTitle());
    System.out.println("处理状态: " + chapter.getProcessStatus());
    System.out.println("Cypher执行状态: " + chapter.getCypherExecuteStatus());
    System.out.println("段落数: " + chapter.getParagraphCount());
}
```

**用途：**
- 审计LLM生成的Cypher质量
- 调试Cypher语句错误
- 对比不同版本的Cypher

### 场景3：重放失败章节的Cypher

```java
@Resource
private GraphService graphService;
@Resource
private ChapterStorageService chapterStorageService;

@Test
public void replayFailedChapters() {
    String bookUuid = "your-book-uuid";
    
    // 步骤1: 查询失败章节
    List<ArticlesEntity> failedChapters = chapterStorageService.queryFailedChapters(bookUuid);
    System.out.println("📋 失败章节数: " + failedChapters.size());
    
    // 步骤2: 重放每个失败章节
    for (ArticlesEntity chapter : failedChapters) {
        System.out.println("\n🔄 重放章节 " + chapter.getChapterIndex() + ": " + chapter.getTitle());
        boolean success = graphService.replayCypherFromMongo(bookUuid, chapter.getChapterIndex());
        
        if (success) {
            System.out.println("✅ 重放成功");
        } else {
            System.out.println("❌ 重放失败，错误: " + chapter.getCypherErrorMessage());
        }
    }
}
```

**重放流程：**

1. 从MongoDB读取已保存的Cypher语句
2. 验证Cypher语法
3. 重新执行到Neo4j
4. 更新MongoDB中的执行状态

**适用场景：**
- Cypher执行失败，需要重试
- 修复了Cypher注入逻辑后，重新执行
- Neo4j连接中断后恢复数据

### 场景4：查询数据统计

```java
@Resource
private GraphService graphService;
@Resource
private ChapterStorageService chapterStorageService;

@Test
public void queryStatistics() {
    String bookUuid = "your-book-uuid";
    String dataSource = "test_epub_40";
    
    // 统计1: MongoDB统计
    ChapterStorageService.BookStats stats = chapterStorageService.getBookStatistics(bookUuid);
    System.out.println("📊 MongoDB统计:");
    System.out.println("  总章节数: " + stats.getTotalChapters());
    System.out.println("  已完成: " + stats.getCompletedChapters());
    System.out.println("  失败: " + stats.getFailedChapters());
    System.out.println("  成功Cypher: " + stats.getSuccessCypherCount());
    
    // 统计2: Neo4j统计
    String neo4jStats = graphService.queryTestDataStats(dataSource);
    System.out.println(neo4jStats);
}
```

**输出示例：**

```
📊 数据统计报告
========================================
数据源: test_epub_40
MongoDB 统计:
  总章节数: 40
  已完成: 38
  失败: 2
  待处理: 0
Neo4j 统计:
  Entity节点数: 152
  Event节点数: 98
  State节点数: 67
========================================
```

### 场景5：清理测试数据

```java
@Resource
private GraphService graphService;

@Test
public void cleanupData() {
    String dataSource = "test_epub_40";
    
    // 步骤1: 清理前查询统计
    System.out.println("🔍 清理前数据统计:");
    graphService.queryTestDataStats(dataSource);
    
    // 步骤2: 执行清理
    CleanupReport report = graphService.cleanupTestData(dataSource);
    
    // 步骤3: 查看清理报告
    System.out.println(report);
    
    // 步骤4: 清理后验证
    System.out.println("\n🔍 清理后数据统计:");
    graphService.queryTestDataStats(dataSource);
}
```

**清理范围：**
- **Neo4j**：删除所有 `dataSource = 'test_epub_40'` 的节点和关系
- **MongoDB**：删除所有 `dataSource = 'test_epub_40'` 的文档

**输出示例：**

```
🧹 数据清理报告
========================================
数据源: test_epub_40
Neo4j删除节点: 317
Neo4j删除关系: 245
MongoDB删除文档: 40
清理耗时: 2.3 秒
清理时间: 2025-01-15T10:30:00Z
========================================
```

## 数据隔离机制

### 隔离策略

本系统采用**多租户数据隔离模式**，通过以下维度实现隔离：

| 存储系统 | 隔离策略 | 实现方式 | 示例 |
|---------|---------|---------|------|
| **Neo4j** | 节点属性标签 | 所有节点添加 `dataSource` 属性 | `{dataSource: 'test_epub_40', bookName: '斗破苍穹'}` |
| **MongoDB** | 属性过滤 | 通过 `dataSource` 字段区分 | `{dataSource: 'test_epub_40'}` |

### 元数据注入示例

**原始Cypher（LLM生成）：**

```cypher
CREATE (e:Entity:Character {name: '萧炎', age: 15})
CREATE (ev:Event:StoryEvent {timestamp: datetime('2025-01-01T00:00:00'), description: '萧炎被退婚'})
```

**注入后的Cypher：**

```cypher
CREATE (e:Entity:Character {name: '萧炎', age: 15, dataSource: 'test_epub_40', bookName: '斗破苍穹', bookUuid: 'uuid-xxx', mongoDocId: 'doc-id-xxx'})
CREATE (ev:Event:StoryEvent {timestamp: datetime('2025-01-01T00:00:00'), description: '萧炎被退婚', dataSource: 'test_epub_40', bookName: '斗破苍穹', bookUuid: 'uuid-xxx', mongoDocId: 'doc-id-xxx'})
```

### 查询隔离数据

**Neo4j查询示例：**

```cypher
// 查询测试数据的所有实体
MATCH (n:Entity {dataSource: 'test_epub_40'}) RETURN n

// 查询测试数据的所有事件
MATCH (e:Event {dataSource: 'test_epub_40'}) RETURN e

// 统计测试数据节点数
MATCH (n {dataSource: 'test_epub_40'}) RETURN count(n)
```

**MongoDB查询示例：**

```javascript
// 查询所有测试章节
db.Articles_store.find({dataSource: 'test_epub_40'})

// 查询失败的章节
db.Articles_store.find({
  dataSource: 'test_epub_40',
  cypherExecuteStatus: 'FAILED'
})

// 统计成功率
db.Articles_store.aggregate([
  {$match: {dataSource: 'test_epub_40'}},
  {$group: {
    _id: '$cypherExecuteStatus',
    count: {$sum: 1}
  }}
])
```

## MongoDB与Neo4j数据关联

### 关联方式

通过双向引用实现关联：

1. **MongoDB → Neo4j**：通过 `bookUuid`、`chapterIndex`、`dataSource` 关联
2. **Neo4j → MongoDB**：通过节点的 `mongoDocId` 属性回溯

### 关联查询示例

**场景：从Neo4j节点回溯MongoDB文档**

```cypher
// 1. 在Neo4j中查询节点
MATCH (e:Entity:Character {name: '萧炎', dataSource: 'test_epub_40'}) 
RETURN e.mongoDocId as docId
```

然后在MongoDB中：

```javascript
// 2. 使用docId查询MongoDB
db.Articles_store.findOne({_id: 'doc-id-from-neo4j'})
```

**场景：从MongoDB文档追踪Neo4j节点**

```javascript
// 1. 在MongoDB中查询章节
var chapter = db.Articles_store.findOne({
  bookUuid: 'uuid-xxx',
  chapterIndex: 1
})
```

然后在Neo4j中：

```cypher
// 2. 使用mongoDocId查询Neo4j
MATCH (n {mongoDocId: 'doc-id-from-mongo'}) RETURN n
```

## 故障排查

### 问题1：LLM调用失败

**症状：**
```
❌ 章节 5 LLM调用失败: Connection timeout
```

**解决方案：**
1. 检查Ollama服务是否启动
2. 检查网络连接
3. 查看LLM配置是否正确
4. 使用重放功能重试失败章节

### 问题2：Cypher验证失败

**症状：**
```
⚠️ 章节 10 验证失败，跳过执行
```

**原因：**
- Cypher语句包含 `paragraphIndex` 属性（章节级处理不应有此属性）
- `source` 格式包含段落标记 " - P"

**解决方案：**
1. 检查LLM生成的Cypher是否符合规范
2. 查看MongoDB中保存的原始Cypher：
   ```java
   String cypher = chapterStorageService.queryCypherByChapter(bookUuid, 10);
   System.out.println(cypher);
   ```
3. 手动修正Cypher后重放

### 问题3：Neo4j执行失败

**症状：**
```
❌ 章节 15 Cypher执行失败: Syntax error
```

**解决方案：**
1. 查看错误详情：
   ```java
   ArticlesEntity chapter = chapterStorageService.queryChapterByIndex(bookUuid, 15);
   System.out.println("错误信息: " + chapter.getCypherErrorMessage());
   ```
2. 检查Cypher语法
3. 在Neo4j Browser中手动执行验证
4. 修正后使用重放功能

### 问题4：数据清理不干净

**症状：**
清理后仍能查询到测试数据

**解决方案：**
1. 检查 `dataSource` 是否一致
2. 手动执行清理Cypher：
   ```cypher
   MATCH (n {dataSource: 'test_epub_40'}) DETACH DELETE n
   ```
3. 检查MongoDB：
   ```javascript
   db.Articles_store.deleteMany({dataSource: 'test_epub_40'})
   ```

## 性能优化建议

### 1. 批量处理

当前实现是顺序处理每个章节。如需提升性能，可考虑：

- **并行处理**：使用线程池并行处理多个章节
- **批量MongoDB写入**：每10个章节批量提交一次

### 2. LLM调用优化

- **缓存机制**：相似章节内容可复用Cypher模板
- **超时控制**：设置合理的LLM调用超时时间
- **重试策略**：失败后自动重试3次

### 3. Neo4j写入优化

- **事务批处理**：单个章节的所有Cypher在一个事务内执行
- **索引优化**：为常用查询字段创建索引

## 配置说明

### 1. EPUB文件路径

在测试类中修改EPUB文件路径：

```java
String epubPath = "C:\\Users\\Shuan\\Downloads\\斗破苍穹-天蚕土豆.epub";
```

### 2. 章节数量限制

修改 `chapterLimit` 参数：

```java
ProcessReport report = graphService.readStoryWithLimit(epubPath, 40, metadata);
//                                                              ↑
//                                                          修改为所需章节数
```

### 3. 数据源标识

修改 `dataSource` 以区分不同测试：

```java
metadata.setDataSource("test_epub_40");    // 测试40章
metadata.setDataSource("test_epub_100");   // 测试100章
metadata.setDataSource("prod_doupo");      // 生产数据
```

## 最佳实践

### 1. 测试前清理旧数据

```java
// 清理上次测试数据
graphService.cleanupTestData("test_epub_40");

// 执行新测试
ProcessReport report = graphService.readStoryWithLimit(...);
```

### 2. 分阶段处理

对于大量章节，建议分批处理：

```java
// 第一批：1-40章
metadata.setDataSource("test_epub_batch1");
graphService.readStoryWithLimit(epubPath, 40, metadata);

// 第二批：41-80章（需修改逻辑支持起始章节）
metadata.setDataSource("test_epub_batch2");
// 未来扩展: graphService.readStoryWithRange(epubPath, 41, 80, metadata);
```

### 3. 定期统计分析

```java
// 每处理10个章节，查看统计
if (chapterIndex % 10 == 0) {
    BookStats stats = chapterStorageService.getBookStatistics(bookUuid);
    System.out.println("当前进度: " + stats.getCompletedChapters() + "/" + stats.getTotalChapters());
}
```

### 4. 错误处理策略

```java
// 处理失败章节
List<ArticlesEntity> failedChapters = chapterStorageService.queryFailedChapters(bookUuid);
if (!failedChapters.isEmpty()) {
    System.out.println("⚠️  发现 " + failedChapters.size() + " 个失败章节");
    
    // 策略1: 自动重试
    for (ArticlesEntity chapter : failedChapters) {
        graphService.replayCypherFromMongo(bookUuid, chapter.getChapterIndex());
    }
    
    // 策略2: 人工介入
    // 导出失败章节列表供人工分析
}
```

## 常见问题

**Q1: 如何修改LLM模型？**

A: 修改GraphService中的注入配置：

```java
@Resource(name = "decomposeLanguageModel")
private OllamaChatModel decomposeLanguageModel;
```

**Q2: 如何支持其他EPUB文件？**

A: 只需修改元数据和文件路径：

```java
metadata.setBookName("诛仙");
String epubPath = "path/to/诛仙.epub";
```

**Q3: MongoDB中数据量过大怎么办？**

A: 可以：
1. 定期归档历史数据
2. 删除不再需要的测试数据
3. 为常用字段创建索引

**Q4: 如何验证数据一致性？**

A: 对比MongoDB和Neo4j的统计：

```java
BookStats mongoStats = chapterStorageService.getBookStatistics(bookUuid);
String neo4jStats = graphService.queryTestDataStats(dataSource);

// 检查MongoDB成功数是否等于Neo4j节点数
```

## 附录

### 数据结构示例

#### MongoDB文档示例

```json
{
  "_id": "uuid-generated-id",
  "title": "第一章 落魄天才",
  "content": "段落1\n段落2\n...",
  "bookUuid": "uuid-doupo-123",
  "chapterIndex": 1,
  "cypherStatements": "CREATE (e:Entity:Character {...})...",
  "cypherExecuteStatus": "SUCCESS",
  "cypherExecuteTime": "2025-01-15T10:31:00Z",
  "cypherErrorMessage": null,
  "processStatus": "COMPLETED",
  "paragraphCount": 85,
  "dataSource": "test_epub_40",
  "createTime": "2025-01-15T10:30:00Z",
  "tags": "{\"bookName\":\"斗破苍穹\",\"dataSource\":\"test_epub_40\"}"
}
```

#### Neo4j节点示例

```cypher
// Entity节点
{
  name: '萧炎',
  age: 15,
  firstMentionChapter: 1,
  firstMentionSource: '第1章 落魄天才',
  dataSource: 'test_epub_40',
  bookName: '斗破苍穹',
  bookUuid: 'uuid-doupo-123',
  mongoDocId: 'uuid-generated-id'
}

// Event节点
{
  timestamp: datetime('2025-01-01T00:00:00'),
  description: '萧炎被退婚',
  chapterIndex: 1,
  source: '第1章 落魄天才',
  dataSource: 'test_epub_40',
  bookName: '斗破苍穹',
  bookUuid: 'uuid-doupo-123',
  mongoDocId: 'uuid-generated-id'
}
```

## 总结

本功能实现了完整的EPUB章节限制读取和知识图谱构建流程，核心特性：

✅ **数据持久化**：MongoDB存储章节内容和Cypher语句  
✅ **数据隔离**：通过元数据实现测试与生产数据隔离  
✅ **可追溯性**：支持从Neo4j节点回溯到MongoDB源文档  
✅ **容错机制**：支持Cypher重放、失败重试  
✅ **统计分析**：多维度数据统计查询  
✅ **数据清理**：快速清理测试数据  

使用时请参考本文档中的使用指南和最佳实践。
