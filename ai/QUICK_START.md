# 快速开始 - EPUB前40章知识图谱构建

## 一分钟快速启动

### 1. 前置条件检查

确保以下服务已启动：

```bash
# Neo4j
# 地址: bolt://8.138.204.38:7687
# 用户名: neo4j
# 密码: Sl123456

# MongoDB
# 已配置在 application.yaml

# Ollama LLM服务
# decomposeLanguageModel 已配置
```

### 2. 运行测试

打开测试类：`EpubChapterLimitTest.java`

```java
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
    
    // 步骤2: 验证元数据
    metadata.validate();
    
    // 步骤3: 读取EPUB并构建图谱
    String epubPath = "C:\\Users\\Shuan\\Downloads\\斗破苍穹-天蚕土豆.epub";
    ProcessReport report = graphService.readStoryWithLimit(epubPath, 40, metadata);
    
    // 步骤4: 查看报告
    System.out.println(report);
}
```

**修改EPUB路径**：将 `epubPath` 修改为你的EPUB文件路径

**运行测试**：右键点击方法 → Run Test

### 3. 预期输出

```
📚 开始处理《斗破苍穹》前 40 个章节...
✅ 已处理章节 1/40: 第一章 落魄天才 (耗时: 5234ms)
✅ 已处理章节 2/40: 第二章 异火 (耗时: 4987ms)
...
✅ 已处理章节 40/40: 第四十章 战斗 (耗时: 5123ms)

📊 知识图谱构建完成！共处理 40 个章节

📊 知识图谱构建报告
========================================
书籍信息: 斗破苍穹 (uuid-xxx)
处理章节: 40 章
成功: 38 | 失败: 2 | 跳过: 0
总耗时: 215.6 秒
平均每章: 5.4 秒
========================================
```

## 核心功能使用

### 功能1: 查询统计信息

```java
// MongoDB统计
BookStats stats = chapterStorageService.getBookStatistics(bookUuid);
System.out.println(stats);

// Neo4j统计
String neo4jStats = graphService.queryTestDataStats("test_epub_40");
System.out.println(neo4jStats);
```

### 功能2: 重放失败章节

```java
// 查询失败章节
List<ArticlesEntity> failed = chapterStorageService.queryFailedChapters(bookUuid);

// 重放
for (var chapter : failed) {
    graphService.replayCypherFromMongo(bookUuid, chapter.getChapterIndex());
}
```

### 功能3: 查询章节Cypher

```java
String cypher = chapterStorageService.queryCypherByChapter(bookUuid, 1);
System.out.println(cypher);
```

### 功能4: 清理测试数据

```java
CleanupReport cleanup = graphService.cleanupTestData("test_epub_40");
System.out.println(cleanup);
```

## 数据查询

### MongoDB查询

```javascript
// 查询所有测试章节
db.Articles_store.find({dataSource: 'test_epub_40'})

// 查询失败章节
db.Articles_store.find({
  dataSource: 'test_epub_40',
  cypherExecuteStatus: 'FAILED'
})

// 统计
db.Articles_store.count({dataSource: 'test_epub_40'})
```

### Neo4j查询

```cypher
// 查询所有实体
MATCH (n:Entity {dataSource: 'test_epub_40'}) RETURN n

// 查询所有事件
MATCH (e:Event {dataSource: 'test_epub_40'}) RETURN e

// 统计节点数
MATCH (n {dataSource: 'test_epub_40'}) RETURN count(n)
```

## 常见问题

### Q: EPUB文件路径不正确？

**A**: 修改测试类中的路径：
```java
String epubPath = "你的EPUB文件路径";
```

### Q: LLM调用失败？

**A**: 检查Ollama服务是否启动，查看配置：
```java
@Resource(name = "decomposeLanguageModel")
private OllamaChatModel decomposeLanguageModel;
```

### Q: MongoDB连接失败？

**A**: 检查 `application.yaml` 中的MongoDB配置

### Q: Neo4j连接失败？

**A**: 检查GraphService中的连接配置：
```java
private static final String NEO4J_URI = "bolt://8.138.204.38:7687";
private static final String NEO4J_USER = "neo4j";
private static final String NEO4J_PASSWORD = "Sl123456";
```

## 完整工作流程

### 1. 首次运行

```java
// 清理旧数据（可选）
graphService.cleanupTestData("test_epub_40");

// 构建图谱
ProcessReport report = graphService.readStoryWithLimit(epubPath, 40, metadata);

// 查看统计
BookStats stats = chapterStorageService.getBookStatistics(bookUuid);
```

### 2. 处理失败章节

```java
// 查询失败章节
List<ArticlesEntity> failed = chapterStorageService.queryFailedChapters(bookUuid);

// 重放失败章节
for (var chapter : failed) {
    boolean success = graphService.replayCypherFromMongo(bookUuid, chapter.getChapterIndex());
    if (!success) {
        // 查看错误信息
        System.out.println("错误: " + chapter.getCypherErrorMessage());
        // 查看Cypher语句
        System.out.println("Cypher: " + chapter.getCypherStatements());
    }
}
```

### 3. 验证结果

```java
// MongoDB验证
BookStats stats = chapterStorageService.getBookStatistics(bookUuid);
System.out.println("成功率: " + (stats.getCompletedChapters() * 100.0 / stats.getTotalChapters()) + "%");

// Neo4j验证
graphService.queryTestDataStats("test_epub_40");
```

### 4. 清理数据

```java
// 测试完成后清理
CleanupReport cleanup = graphService.cleanupTestData("test_epub_40");
System.out.println("删除MongoDB文档: " + cleanup.getMongoDocsDeleted());
```

## 文件位置

| 文件 | 路径 |
|------|------|
| 测试类 | `ai/src/test/java/EpubChapterLimitTest.java` |
| GraphService | `ai/src/main/java/com/shuanglin/bot/service/GraphService.java` |
| ChapterStorageService | `ai/src/main/java/com/shuanglin/bot/service/ChapterStorageService.java` |
| 使用文档 | `ai/EPUB_CHAPTER_LIMIT_USAGE.md` |
| 实现总结 | `ai/IMPLEMENTATION_SUMMARY.md` |

## 下一步

- 📖 阅读详细文档: `EPUB_CHAPTER_LIMIT_USAGE.md`
- 🔧 查看实现细节: `IMPLEMENTATION_SUMMARY.md`
- 🧪 运行所有测试: `EpubChapterLimitTest.java`
- 📊 分析知识图谱: Neo4j Browser

## 技术支持

如遇问题，请检查：

1. ✅ 所有服务是否启动（Neo4j, MongoDB, Ollama）
2. ✅ EPUB文件路径是否正确
3. ✅ 网络连接是否正常
4. ✅ 配置是否正确

详细故障排查请参考 `EPUB_CHAPTER_LIMIT_USAGE.md` 的"故障排查"章节。
