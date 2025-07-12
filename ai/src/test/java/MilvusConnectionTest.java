import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MilvusConnectionTest {

	// --- 1. 配置参数 ---
	private static final String HOST = "172.18.32.160";
	private static final int PORT = 19530;
	private static final String COLLECTION_NAME = "rag-embedding-collection";
	private static final int VECTOR_DIM = 8;
	private static final int NUM_ENTITIES = 3000;
	private static final int TOP_K = 5;

	public static void main(String[] args) {
		// --- 2. 连接 Milvus ---
		System.out.println("🔗 开始连接 Milvus (" + HOST + ":" + PORT + ")...");
		MilvusServiceClient milvusClient = new MilvusServiceClient(
				ConnectParam.newBuilder()
						.withHost(HOST)
						.withPort(PORT)
						.build()
		);
		System.out.println("✅ 成功连接到 Milvus!");

		try {
			// --- 3. 清理旧的 Collection (如果存在) ---
			R<Boolean> hasCollectionResponse = milvusClient.hasCollection(
					HasCollectionParam.newBuilder().withCollectionName(COLLECTION_NAME).build()
			);
			if (hasCollectionResponse.getData()!=null) {
				System.out.println("🧹 发现已存在的 Collection '" + COLLECTION_NAME + "'，正在删除...");
				milvusClient.dropCollection(
						DropCollectionParam.newBuilder().withCollectionName(COLLECTION_NAME).build()
				);
				System.out.println("🗑️ 旧的 Collection 已删除。");
			}

			// --- 4. 创建 Collection (已修正) ---
			System.out.println("➕ 正在创建 Collection '" + COLLECTION_NAME + "'...");
			FieldType primaryKeyField = FieldType.newBuilder()
					.withName("pk_id")
					.withDataType(DataType.Int64)
					.withPrimaryKey(true)
					.withAutoID(false) // 我们将手动提供ID
					.build();

			FieldType vectorField = FieldType.newBuilder()
					.withName("embeddings")
					.withDataType(DataType.FloatVector)
					.withDimension(VECTOR_DIM)
					.build();

			// 将所有字段放入一个 List 中
			List<FieldType> fieldsSchema = new ArrayList<>();
			fieldsSchema.add(primaryKeyField);
			fieldsSchema.add(vectorField);

			// 使用 .withFieldTypes() 方法
			CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
					.withCollectionName(COLLECTION_NAME)
					.withDescription("A simple demo collection for Java")
					.withFieldTypes(fieldsSchema) // <--- 这里是修正的关键点
					.build();
			milvusClient.createCollection(createCollectionParam);
			System.out.println("✅ Collection 创建成功！");


			// --- 5. 插入数据 ---
			System.out.println("📨 正在生成并插入 " + NUM_ENTITIES + " 条数据...");
			List<Long> pkIds = new ArrayList<>();
			List<List<Float>> vectors = new ArrayList<>();
			Random rand = new Random();
			for (long i = 0L; i < NUM_ENTITIES; ++i) {
				pkIds.add(i);
				List<Float> vector = new ArrayList<>();
				for (int d = 0; d < VECTOR_DIM; ++d) {
					vector.add(rand.nextFloat());
				}
				vectors.add(vector);
			}

			List<InsertParam.Field> fieldsData = new ArrayList<>();
			fieldsData.add(new InsertParam.Field("pk_id", pkIds));
			fieldsData.add(new InsertParam.Field("embeddings", vectors));

			InsertParam insertParam = InsertParam.newBuilder()
					.withCollectionName(COLLECTION_NAME)
					.withFields(fieldsData)
					.build();
			milvusClient.insert(insertParam);

			// 必须 flush 使数据可见
			milvusClient.flush(FlushParam.newBuilder().addCollectionName(COLLECTION_NAME).build());
			System.out.println("✅ 数据插入并 Flush 完成！");

			// --- 6. 创建索引 ---
			System.out.println("🏗️ 正在为向量字段创建索引...");
			milvusClient.createIndex(
					CreateIndexParam.newBuilder()
							.withCollectionName(COLLECTION_NAME)
							.withFieldName("embeddings")
							.withIndexType(IndexType.IVF_FLAT) // 一个常用的索引类型
							.withMetricType(MetricType.L2)   // 欧式距离
							.withExtraParam("{\"nlist\":128}") // 索引特有的参数
							.build()
			);
			System.out.println("✅ 索引创建指令已发送。");

			// --- 7. 加载 Collection 到内存 ---
			System.out.println("💾 正在加载 Collection 到内存中以供搜索...");
			milvusClient.loadCollection(
					LoadCollectionParam.newBuilder().withCollectionName(COLLECTION_NAME).build()
			);
			System.out.println("✅ Collection 加载完成！");

			// --- 8. 执行向量搜索 ---
			System.out.println("🔍 正在执行向量搜索...");
			List<List<Float>> queryVectors = new ArrayList<>();
			queryVectors.add(vectors.get(0)); // 用第一条插入的数据作为查询向量

			SearchParam searchParam = SearchParam.newBuilder()
					.withCollectionName(COLLECTION_NAME)
					.withMetricType(MetricType.L2)
					.withTopK(TOP_K)
					.withVectors(queryVectors)
					.withVectorFieldName("embeddings")
					.withParams("{\"nprobe\":10}") // 搜索特有的参数
					.build();
			R<SearchResults> response = milvusClient.search(searchParam);

			// --- 9. 打印搜索结果 ---
			System.out.println("\n🎉 搜索完成！结果如下：");
			SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
			// 因为我们只查询了一个向量，所以结果在索引 0
			List<SearchResultsWrapper.IDScore> results = wrapper.getIDScore(0);
			for (SearchResultsWrapper.IDScore result : results) {
				System.out.println("  -> ID: " + result.getLongID() + ", 距离(Score): " + result.getScore());
			}

		} finally {
			// --- 10. 清理和断开连接 ---
			milvusClient.close();
		}
	}
}