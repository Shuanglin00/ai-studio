package com.shuanglin.bot.langchain4j.rag.config;

import io.milvus.param.Constant;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.exception.MilvusClientException;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DescribeCollectionReq;
import io.milvus.v2.service.database.request.CreateDatabaseReq;
import io.milvus.v2.service.database.request.DescribeDatabaseReq;
import io.milvus.v2.service.database.response.DescribeDatabaseResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static io.milvus.v2.common.DataType.*;

@Component
@Slf4j
public class MilvusInitializer implements ApplicationRunner {

	@Resource
	private MilvusClientV2 milvusClient;

	@Value("${spring.data.milvus.defaultDatabaseName}")
	private String defaultDatabaseName; // 默认数据库名

	@Value("${spring.data.milvus.defaultCollectionName}")
	private String defaultCollectionName; // 默认集合名

	@Override
	public void run(ApplicationArguments args) {
		log.info("--- 开始进行 Milvus 初始化检查 ---");
		try {
			try {
				DescribeDatabaseResp descDBResp = milvusClient.describeDatabase(DescribeDatabaseReq.builder()
						.databaseName(defaultDatabaseName)
						.build());
			} catch (MilvusClientException notFoundException) {
				log.info("数据库 '{}' 不存在，正在创建...", defaultDatabaseName);
				milvusClient.createDatabase(CreateDatabaseReq.builder().databaseName(defaultDatabaseName).build());
				log.info("数据库 '{}' 创建成功。", defaultDatabaseName);
			} catch (Exception e) {
				log.warn("无法检查或创建数据库 '{}'。这可能是因为您的 Milvus 版本较旧或权限问题。将使用 'default' 数据库继续。", defaultDatabaseName, e);
			}

			// 切换客户端上下文到目标数据库，后续操作都在此数据库中进行
			milvusClient.useDatabase(defaultDatabaseName);
			log.info("当前操作的数据库为: '{}'", this.defaultDatabaseName);

			// 2. 检查 Collection 是否已存在
			try {
				DescribeCollectionReq request = DescribeCollectionReq.builder()
						.collectionName(defaultCollectionName)
						.build();
				milvusClient.describeCollection(request);
			} catch (Exception e) {

				// 3. 如果 Collection 不存在，则创建它
				log.info("Collection '{}' 不存在，开始创建...", defaultCollectionName);

				CreateCollectionReq.CollectionSchema schema = MilvusClientV2.CreateSchema();
				schema.addField(AddFieldReq.builder()
						.fieldName("userId")
						.dataType(VarChar)
						.build()
				);
				schema.addField(AddFieldReq.builder()
						.fieldName("groupId")
						.dataType(VarChar)
						.build()
				);
				schema.addField(AddFieldReq.builder()
						.fieldName("embeddings")
						.dataType(FloatVector)
						.dimension(1536) // 根据您的模型设置向量维度
						.build()
				);
				schema.addField(AddFieldReq.builder()
						.fieldName("memoryId")
						.dataType(Int64)
						.isPrimaryKey(true)
						.build()
				);

				IndexParam indexParamForUserField = IndexParam.builder()
						.fieldName("userId")
						.indexType(IndexParam.IndexType.AUTOINDEX)
						.build();

				IndexParam indexParamForGroupField = IndexParam.builder()
						.fieldName("groupId")
						.indexType(IndexParam.IndexType.AUTOINDEX)
						.build();

				IndexParam indexParamForVectorField = IndexParam.builder()
						.fieldName("embeddings")
						.indexType(IndexParam.IndexType.FLAT)
						.indexName("VectorField")
						.metricType(IndexParam.MetricType.L2)
						.build();

				List<IndexParam> indexParams = new ArrayList<>();
				indexParams.add(indexParamForUserField);
				indexParams.add(indexParamForGroupField);
				indexParams.add(indexParamForVectorField);
				CreateCollectionReq createCollectionParam = CreateCollectionReq.builder()
						.collectionName(defaultCollectionName)
						.description("用于 LangChain4j RAG 的 Collection")
						.indexParams(indexParams)
						.collectionSchema(schema)
						.property(Constant.MMAP_ENABLED, "true") // 启用内存映射
						.build();
				milvusClient.createCollection(createCollectionParam);
				log.info("Collection '{}' 创建成功。", defaultCollectionName);
			}
		} catch (Exception e) {
			log.error("Milvus 初始化失败！应用可能无法正常工作。", e);
		} finally {
			log.info("--- Milvus 初始化检查完成 ---");
		}
	}
}