package com.shuanglin.framework.bus.event;

import com.shuanglin.framework.bus.event.data.Anonymous;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class GroupMessageEvent extends MessageEvent{
	Long groupId;

	Anonymous anonymous;
}
