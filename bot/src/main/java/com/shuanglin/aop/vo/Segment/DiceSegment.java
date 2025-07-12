package com.shuanglin.aop.vo.Segment;

import com.shuanglin.aop.vo.AbstractMessageSegment;
import com.shuanglin.aop.vo.Segment.data.DiceData;
import com.shuanglin.aop.vo.Segment.data.ForwardData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiceSegment extends AbstractMessageSegment {
    private DiceData data;
    // Constructors, Getters, Setters...
}
