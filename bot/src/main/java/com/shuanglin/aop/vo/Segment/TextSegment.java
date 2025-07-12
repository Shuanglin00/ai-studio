package com.shuanglin.aop.vo.Segment;

import com.shuanglin.aop.vo.AbstractMessageSegment;
import com.shuanglin.aop.vo.Segment.data.TextData;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TextSegment extends AbstractMessageSegment {
	private TextData data;
}