package com.shuanglin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MongoDBConstant {
	@Getter
	@AllArgsConstructor
	@NoArgsConstructor
	public enum StoreType {
		memory, document, nonMemory
	}
}
