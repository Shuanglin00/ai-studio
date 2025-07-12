package com.shuanglin.aop.vo.Segment;

import com.shuanglin.aop.vo.AbstractMessageSegment;
import com.shuanglin.aop.vo.Segment.data.RecordData;
import com.shuanglin.aop.vo.Segment.data.ShakeData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShakeSegment extends AbstractMessageSegment {
    private ShakeData data;
    // Constructors, Getters, Setters...
}