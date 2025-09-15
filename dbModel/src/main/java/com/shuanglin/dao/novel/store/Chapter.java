package com.shuanglin.dao.novel.store;

import com.shuanglin.dao.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;


import java.io.Serializable;


@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Document(collection = "chapters")
/**
 章节实体类
 */
public class Chapter extends BaseEntity implements Serializable {
  // 生成一个实体类 需要使用私有定义变量，使用lombok  private String content;
  /**
   * 章节ID
   */
  private String id;

  /**
   * 小说名称
   */
  private String novelName;

  /**   * 章节名称
   */
  private String chapterName;

  /**
   * 小说ID
   */
  private String novelId;

  /**
   * 章节内容
   */
  private String content;

  /**
   * 章节描述
   */
  private String description;

  /**
   * 章节cypher信息
   */
  private String cypherDescription;

  /**
   * 章节序号
   */
  private Integer chapterNumber;

  /**
   * 上一章内容
   */
  private String lastChapterContent;

}
