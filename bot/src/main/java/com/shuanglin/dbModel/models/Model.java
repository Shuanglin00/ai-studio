package com.shuanglin.dbModel.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("model" )
public class Model implements Serializable {

	private String id;

	private String modelName; //名称

	private String modelType; //类型

	private String description; //描述

	private String instruction; //任务指令

	private String constraints; // 限制与要求

	private String isActive;


}
