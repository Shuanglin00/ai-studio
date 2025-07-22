package com.shuanglin.framework.bus.event.data;

import lombok.Data;

@Data
public class Sender {
	String userId;     // 发送者 QQ 号
	String nickname;   // 昵称
	String card;       // 群名片／备注
	String sex;        // 性别，male 或 female 或 unknown
	Integer age;           // 年龄
	String area;       // 地区
	String level;      // 成员等级
	String role;       // 角色，owner 或 admin 或 member
	String title;       // 专属头衔
}
