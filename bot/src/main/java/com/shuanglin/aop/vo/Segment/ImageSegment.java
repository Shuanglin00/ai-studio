package com.shuanglin.aop.vo.Segment;

import com.shuanglin.aop.vo.AbstractMessageSegment;
import com.shuanglin.aop.vo.Segment.data.ImageData;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageSegment extends AbstractMessageSegment {
	// getters and setters...
	private ImageData data;

}