package com.shuanglin.aop.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MessageEvent extends Event{
	String message;
}
