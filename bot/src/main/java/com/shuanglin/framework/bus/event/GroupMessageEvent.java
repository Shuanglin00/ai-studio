package com.shuanglin.framework.bus.event;

import com.google.gson.annotations.SerializedName;
import com.shuanglin.framework.bus.event.data.Anonymous;
import lombok.*;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class GroupMessageEvent extends MessageEvent implements Serializable {
	@SerializedName("group_id")
	Long groupId;

	@SerializedName("anonymous")
	Anonymous anonymous;
}
