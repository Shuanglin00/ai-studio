package com.shuanglin.dao.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ModelInfo extends Model {

	private List<String> activeModels;

}
