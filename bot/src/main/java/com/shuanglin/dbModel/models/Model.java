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

	private String modelName;

	private String modelType;

	private String description;

	private String isActive;


}
