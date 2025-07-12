package com.shuanglin.aop.vo.Segment;

import com.shuanglin.aop.vo.AbstractMessageSegment;
import com.shuanglin.aop.vo.Segment.data.JsonData;
import com.shuanglin.aop.vo.Segment.data.NodeData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JsonSegment extends AbstractMessageSegment {
    private JsonData data;
    // Constructors, Getters, Setters...
}