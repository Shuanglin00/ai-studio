package com.shuanglin.event.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data // 使用Lombok简化getter/setter/toString
public class Sender {
	@JsonProperty("user_id")
	private long userId;

	@JsonProperty("nickname")
	private String nickname;

	@JsonProperty("card")
	private String card; // 群名片

	@JsonProperty("role")
	private String role; // "owner", "admin", "member"
}