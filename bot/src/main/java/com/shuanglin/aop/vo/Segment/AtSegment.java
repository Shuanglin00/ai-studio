package com.shuanglin.aop.vo.Segment;

import com.shuanglin.aop.vo.AbstractMessageSegment;
import com.shuanglin.aop.vo.Segment.data.AtData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AtSegment extends AbstractMessageSegment {
	private AtData data;
}