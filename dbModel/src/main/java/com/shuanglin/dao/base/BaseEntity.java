package com.shuanglin.dao.base;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString
public class BaseEntity {
  /**
   * 是否删除
   * 0：未删除 1：已删除
   */

  private Boolean deleted;

  /**
   * 创建时间
   */
  private LocalDateTime createTime;

  /**
   * 更新时间
   */
  private LocalDateTime updateTime;
}
