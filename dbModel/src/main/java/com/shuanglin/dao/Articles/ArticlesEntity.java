package com.shuanglin.dao.Articles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("Articles_store")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ArticlesEntity {
	private String id;

	private String title;

	private String content;

	private String tags;

	private String createTime;
}
