package com.shuanglin.dbModel.info;

import com.shuanglin.dbModel.models.Model;
import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor()
@NoArgsConstructor
@Builder
public class ModelInfo extends Model {

	private String useModel;
	private List<String> activeModels;

}
