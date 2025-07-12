package com.shuanglin.aop.vo.Segment.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonData {
    private String file;
    private String url;
    private Boolean cache;
    private Boolean proxy;
    private Integer timeout;
    
}