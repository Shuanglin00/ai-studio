package com.shuanglin.aop.vo.Segment.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageData {
	// 假设JSON中的key是 "file"，我们映射到 "fileUrl" 字段
	@JsonProperty("file")
	private String fileUrl;

	@JsonProperty("file")
	private String fileData;

}
