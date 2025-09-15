package com.shuanglin.dao.milvus;

import com.google.gson.annotations.SerializedName;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.milvus.plus.annotation.ExtraParam;
import org.dromara.milvus.plus.annotation.MilvusCollection;
import org.dromara.milvus.plus.annotation.MilvusField;
import org.dromara.milvus.plus.annotation.MilvusIndex;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@MilvusCollection(name = "chatEmbeddingCollection")
public class MessageEmbeddingEntity {
	@MilvusField(
			name = "id", // 字段名称
			dataType = DataType.VarChar, // 数据类型为64位整数
			isPrimaryKey = true, // 标记为主键
			autoID = true
	)
	private String id;

	@MilvusField(
			name = "userId", // 字段名称
			dataType = DataType.VarChar, // 数据类型为64位整数
			nullable = true
	)
	@SerializedName(value = "userId", alternate = {"user_id"})
	private String userId;

	@MilvusField(
			name = "groupId", // 字段名称
			dataType = DataType.VarChar , // 数据类型为64位整数
			nullable = true
	)
	@SerializedName(value = "groupId", alternate = {"group_id"})
	private String groupId;

	@MilvusField(
			name = "storeType", // 字段名称
			dataType = DataType.VarChar // 数据类型为64位整数
	)
	private String storeType;

	@MilvusField(
			name = "modelName", // 字段名称
			dataType = DataType.VarChar, // 数据类型为64位整数
			nullable = true
	)
	private String modelName;

	@MilvusIndex(
			indexType = IndexParam.IndexType.IVF_FLAT, // 使用IVF_FLAT索引类型
			metricType = IndexParam.MetricType.L2, // 使用L2距离度量类型
			indexName = "face_index", // 索引名称
			extraParams = { // 指定额外的索引参数
					@ExtraParam(key = "nlist", value = "100") // 例如，IVF的nlist参数
			}
	)
	@MilvusField(
			name = "embeddings", // 字段名称
			dataType = DataType.FloatVector, // 数据类型为浮点型向量
			dimension = 1536 // 向量维度，假设人脸特征向量的维度是128
	)
	private float[] embeddings;
	@MilvusField(
			name = "storeId", // 字段名称
			dataType = DataType.VarChar // 数据类型为浮点型向量
	)
	private String storeId;


}
