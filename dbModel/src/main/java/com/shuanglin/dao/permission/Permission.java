package com.shuanglin.dao.permission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "permissions")
public class Permission implements Serializable {
	private String id;
	/**
	 * 权限类型 group friend private 針對不同的聊天場景不同的權限分配
	 * 某群拥有某些 指令/模型
	 * 某人拥有某些 指令/模型
	 *
	 */
	private String permissionType;
	private String groupId;
	private String userId;
	private List<String> commands;
	private List<String> models;

}
