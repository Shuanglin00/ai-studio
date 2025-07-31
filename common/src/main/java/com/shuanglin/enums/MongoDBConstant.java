package com.shuanglin.enums;

import lombok.Getter;

public class MongoDBConstant {
	@Getter
	public enum StoreType {
		memory, document, nonMemory
	}
}
