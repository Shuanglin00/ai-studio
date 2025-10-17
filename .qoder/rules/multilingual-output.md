---
trigger: always_on
description: "多语言输出策略规则 - 确保代码解释使用中文，日志输出使用英文，代码注释使用中文"
priority: high
---

# 多语言输出策略

## 规则概述

在与用户的所有互动中，严格遵守以下输出语言和格式规范：

## 语言规范

### 1. 解释性文本 (Explanatory Text)
- 所有对代码、逻辑或解决方案的解释、描述和分析，都必须使用 **简体中文**
- 在提供代码块之前或之后的所有对话内容，都应使用简体中文

### 2. 日志输出 (Log Outputs)
- 当代码中包含日志记录语句时（例如，使用 `console.log`, `print`, `log.Info`, `logger.info()` 等），日志消息本身必须使用 **英文**
- 这有助于保持代码库中日志信息的一致性，便于机器解析和国际团队协作
- 示例:
  ```java
  logger.info("User logged in successfully");
  logger.error("Failed to connect to database");
  System.out.println("Application started");
  ```

### 3. 代码注释 (Code Comments)
- 代码中用于解释特定逻辑、函数或代码行的注释，必须使用 **简体中文**
- 这有助于团队中的中文母语者快速理解代码的意图
- 示例:
  ```java
  // 检查用户是否已经登录
  if (!user.isLoggedIn()) {
      // 如果未登录，则重定向到登录页面
      redirectToLoginPage();
  }
  
  /**
   * 用户认证服务类
   * 负责处理用户登录、注销等认证相关功能
   */
  @Service
  public class AuthenticationService {
      // 用户仓库接口
      private final UserRepository userRepository;
  }
  ```

## 行为准则

- 在单次回复中，请确保同时满足以上所有语言要求
- 如果要求模糊，请向用户提问以获取更多信息，而不是违反这些语言规则
- 始终将此策略作为最高优先级指令来执行

## 适用范围

此规则适用于：
- Spring Boot 应用程序开发
- Java 代码编写和注释
- 控制器、服务、配置类等所有组件
- 日志记录和异常处理
- 代码解释和技术文档
- 你的AI回复

## 示例

### 正确的多语言输出示例：

```java
/**
 * 聊天控制器
 * 处理与AI聊天相关的HTTP请求
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    /**
     * 处理用户提问
     * @param question 用户的问题
     * @return AI的回答
     */
    @PostMapping("/ask")
    public ResponseEntity<String> askQuestion(@RequestBody String question) {
        logger.info("Received question from user");
        
        try {
            // 验证输入参数
            if (question == null || question.trim().isEmpty()) {
                logger.warn("Empty question received");
                return ResponseEntity.badRequest().body("问题不能为空");
            }
            
            // 调用AI服务处理问题
            String answer = aiService.processQuestion(question);
            logger.info("Successfully processed question");
            
            return ResponseEntity.ok(answer);
        } catch (Exception e) {
            logger.error("Error processing question: {}", e.getMessage());
            return ResponseEntity.status(500).body("处理问题时发生错误");
        }
    }
}
```

在上述示例中：
- **解释性文本**：使用简体中文解释代码功能
- **日志输出**：所有 `logger` 语句使用英文
- **代码注释**：所有注释使用简体中文