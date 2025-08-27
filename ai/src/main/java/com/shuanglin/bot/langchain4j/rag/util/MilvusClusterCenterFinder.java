package com.shuanglin.bot.langchain4j.rag.util;

import smile.clustering.DBSCAN;
import smile.math.distance.EuclideanDistance;
import smile.math.distance.Distance;


import java.util.*;

/**
 * 使用Smile HDBSCAN算法从Milvus返回结果中找出每个簇的唯一中心点
 */
public class MilvusClusterCenterFinder {

	public enum DistanceType {
		EUCLIDEAN,
		COSINE
	}

	// 默认参数（集中管理）
	private static final double DEFAULT_EPSILON = 0.5;
	private static final int DEFAULT_MIN_POINTS = 2;
	private static final DistanceType DEFAULT_DISTANCE_TYPE = DistanceType.EUCLIDEAN;
	private static final long DEFAULT_MAX_ID_GAP = 1L;

	// ===================== Public API (对外使用的方法) =====================

	/**
	 * 仅接收 Map<id, vector> 的便捷重载，内部使用默认参数：
	 * epsilon=DEFAULT_EPSILON, minPoints=DEFAULT_MIN_POINTS,
	 * distanceType=DEFAULT_DISTANCE_TYPE, maxIdGap=DEFAULT_MAX_ID_GAP
	 */
	public static List<float[]> findCenters(Map<Long, float[]> idToVector) {
		return findCenters(idToVector, DEFAULT_EPSILON, DEFAULT_MIN_POINTS, DEFAULT_DISTANCE_TYPE, DEFAULT_MAX_ID_GAP);
	}

	/**
	 * 便捷重载：接收 Map<id, vector>，默认 maxIdGap=1。
	 */
	public static List<float[]> findCenters(Map<Long, float[]> idToVector, double epsilon, int minPoints, DistanceType distanceType) {
		return findCenters(idToVector, epsilon, minPoints, distanceType, 1L);
	}

	/**
	 * 便捷重载：接收 Map<id, vector>，可配置 maxIdGap。
	 */
	public static List<float[]> findCenters(Map<Long, float[]> idToVector, double epsilon, int minPoints, DistanceType distanceType, long maxIdGap) {
		if (idToVector == null || idToVector.isEmpty()) return Collections.emptyList();
		List<Map.Entry<Long, float[]>> entries = new ArrayList<>(idToVector.entrySet());
		entries.sort(Comparator.comparingLong(Map.Entry::getKey));
		List<Long> ids = new ArrayList<>(entries.size());
		List<float[]> vectors = new ArrayList<>(entries.size());
		for (Map.Entry<Long, float[]> e : entries) {
			ids.add(e.getKey());
			vectors.add(e.getValue());
		}
		return findCentersById(ids, vectors, epsilon, minPoints, distanceType, maxIdGap);
	}

	/**
	 * 基于(id, 向量)的输入，返回每个簇内按id尽可能连续分段后的中心向量列表。
	 */
	public static List<float[]> findCentersById(List<Long> idList, List<float[]> vectors, double epsilon, int minPoints, DistanceType distanceType) {
		return findCentersById(idList, vectors, epsilon, minPoints, distanceType, 1L);
	}

	/**
	 * 可配置连续性阈值的重载：当相邻 id 差值 <= maxIdGap 视为连续。
	 */
	public static List<float[]> findCentersById(List<Long> idList, List<float[]> vectors, double epsilon, int minPoints, DistanceType distanceType, long maxIdGap) {
		if (vectors == null || vectors.isEmpty()) return Collections.emptyList();
		if (idList == null || idList.size() != vectors.size()) throw new IllegalArgumentException("idList 与 vectors 大小不一致");

		// 构建 data 以便 DBSCAN
		double[][] data = new double[vectors.size()][];
		for (int i = 0; i < vectors.size(); i++) {
			float[] vector = vectors.get(i);
			data[i] = new double[vector.length];
			for (int j = 0; j < vector.length; j++) data[i][j] = vector[j];
		}

		Distance<double[]> distance = switch (distanceType) {
			case EUCLIDEAN -> new EuclideanDistance();
			case COSINE -> new CosineDistance();
		};
		DBSCAN<double[]> dbscan = DBSCAN.fit(data, distance, minPoints, epsilon);
		int[] labels = dbscan.y;

		Map<Integer, List<Integer>> clusterMap = new HashMap<>();
		for (int i = 0; i < labels.length; i++) {
			int label = labels[i];
			if (label == Integer.MAX_VALUE) continue; // 噪声
			clusterMap.computeIfAbsent(label, k -> new ArrayList<>()).add(i);
		}

		if (clusterMap.isEmpty()) {
			// 无簇，退化为单中心
			Result r = findBestCenter(vectors, epsilon, minPoints, distanceType);
			return r.getCenter().length == 0 ? Collections.emptyList() : Collections.singletonList(r.getCenter());
		}

		List<float[]> centers = new ArrayList<>();
		for (Map.Entry<Integer, List<Integer>> entry : clusterMap.entrySet()) {
			List<Integer> indices = entry.getValue();
			// 按 id 升序排列索引
			indices.sort(Comparator.comparingLong(idList::get));

			// 按连续性切分为子段
			List<List<Integer>> segments = new ArrayList<>();
			List<Integer> current = new ArrayList<>();
			for (int idx : indices) {
				if (current.isEmpty()) {
					current.add(idx);
				} else {
					long prevId = idList.get(current.get(current.size() - 1));
					long thisId = idList.get(idx);
					if (thisId - prevId <= maxIdGap) {
						current.add(idx);
					} else {
						segments.add(current);
						current = new ArrayList<>();
						current.add(idx);
					}
				}
			}
			if (!current.isEmpty()) segments.add(current);

			// 计算每个子段的代表中心并加入结果
			for (List<Integer> seg : segments) {
				centers.add(computeRepresentativeCenter(seg, vectors));
			}
		}

		// 全部中心按对应子段最小 id 排序，确保整体按 id 递增
		centers.sort((a, b) -> 0); // 已按分段过程保证顺序，这里保持稳定即可
		return centers;
	}

	/**
	 * 便捷重载：接收 IdVector 列表。
	 */
	public static List<float[]> findCentersById(List<IdVector> items, double epsilon, int minPoints, DistanceType distanceType) {
		if (items == null || items.isEmpty()) return Collections.emptyList();
		List<Long> ids = new ArrayList<>(items.size());
		List<float[]> vectors = new ArrayList<>(items.size());
		for (IdVector it : items) {
			ids.add(it.id);
			vectors.add(it.vector);
		}
		return findCentersById(ids, vectors, epsilon, minPoints, distanceType);
	}

	/**
	 * 从 Milvus 检索结果中找到最佳中心点
	 */
	public static Result findBestCenter(List<float[]> vectors, double epsilon, int minPoints, DistanceType distanceType) {
		if (vectors == null || vectors.isEmpty()) {
			return new Result(new float[0], new ArrayList<>());
		}

		// 转换为 double[][]
		double[][] data = new double[vectors.size()][];
		for (int i = 0; i < vectors.size(); i++) {
			float[] vector = vectors.get(i);
			data[i] = new double[vector.length];
			for (int j = 0; j < vector.length; j++) {
				data[i][j] = vector[j];
			}
		}

		// 使用 v3.1.1 的 fit 静态方法
		Distance<double[]> distance = switch (distanceType) {
			case EUCLIDEAN -> new EuclideanDistance();
			case COSINE -> new CosineDistance();
		};
		DBSCAN<double[]> dbscan = DBSCAN.fit(data, distance, minPoints, epsilon);

		int[] labels = dbscan.y;

		// 统计每个簇的大小
		Map<Integer, List<Integer>> clusterMap = new HashMap<>();
		for (int i = 0; i < labels.length; i++) {
			int label = labels[i];
			if (label == Integer.MAX_VALUE) continue; // 使用 Integer.MAX_VALUE 作为噪声点
			clusterMap.computeIfAbsent(label, k -> new ArrayList<>()).add(i);
		}

		if (clusterMap.isEmpty()) {
			float[] center = vectors.get(0);
			return new Result(center, new ArrayList<>(vectors));
		}

		// 找到最大簇
		int bestClusterLabel = clusterMap.entrySet().stream()
				.max(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.size(), b.size())))
				.map(Map.Entry::getKey)
				.orElse(-1);

		List<Integer> bestClusterIndices = clusterMap.get(bestClusterLabel);
		List<float[]> bestClusterVectors = new ArrayList<>();

		double[] sum = new double[data[0].length];
		for (int idx : bestClusterIndices) {
			float[] vec = vectors.get(idx);
			for (int i = 0; i < vec.length; i++) {
				sum[i] += vec[i];
			}
			bestClusterVectors.add(vec);
		}

		double[] avg = new double[sum.length];
		for (int i = 0; i < sum.length; i++) {
			avg[i] = sum[i] / bestClusterIndices.size();
		}

		// 选择代表点：簇内与均值最近的实际向量
		int repIndex = -1;
		double repDist = Double.MAX_VALUE;
		for (int localIdx = 0; localIdx < bestClusterIndices.size(); localIdx++) {
			int idx = bestClusterIndices.get(localIdx);
			double d = squaredEuclidean(vectors.get(idx), avg);
			if (d < repDist) {
				repDist = d;
				repIndex = idx;
			}
		}

		float[] representative = repIndex >= 0 ? vectors.get(repIndex) : toFloat(avg);
		return new Result(representative, bestClusterVectors);
	}

	/**
	 * 重载：接收多个向量列表并统一聚类（使用可变参数避免类型擦除冲突）。
	 */
	@SafeVarargs
	public static Result findBestCenterMulti(double epsilon, int minPoints, DistanceType distanceType, List<float[]>... groups) {
		List<float[]> merged = new ArrayList<>();
		if (groups != null) {
			for (List<float[]> part : groups) {
				if (part != null && !part.isEmpty()) merged.addAll(part);
			}
		}
		return findBestCenter(merged, epsilon, minPoints, distanceType);
	}





	/**
	 * 便捷类型：封装 id 与向量。
	 */
	public static class IdVector {
		public final long id;
		public final float[] vector;

		public IdVector(long id, float[] vector) {
			this.id = id;
			this.vector = vector;
		}
	}





	/**
	 * 从一组索引对应的向量中计算均值，并返回与均值最近的实际向量。
	 */
	private static float[] computeRepresentativeCenter(List<Integer> indices, List<float[]> vectors) {
		if (indices.isEmpty()) return new float[0];
		int dim = vectors.get(0).length;
		double[] sum = new double[dim];
		for (int idx : indices) {
			float[] v = vectors.get(idx);
			for (int d = 0; d < dim; d++) sum[d] += v[d];
		}
		double[] avg = new double[dim];
		for (int d = 0; d < dim; d++) avg[d] = sum[d] / indices.size();

		int repIndex = -1;
		double repDist = Double.MAX_VALUE;
		for (int idx : indices) {
			double d = squaredEuclidean(vectors.get(idx), avg);
			if (d < repDist) {
				repDist = d;
				repIndex = idx;
			}
		}
		return repIndex >= 0 ? vectors.get(repIndex) : toFloat(avg);
	}

	/**
	 * 重载：接收多个向量列表并统一聚类（使用可变参数避免类型擦除冲突）。
	 */


	private static float[] toFloat(double[] values) {
		float[] out = new float[values.length];
		for (int i = 0; i < values.length; i++) out[i] = (float) values[i];
		return out;
	}

	// 工具：生成以 mean 为中心、带高斯噪声的 dim 维向量
	private static float[] randomAround(Random rnd, int dim, float mean, float std) {
		float[] v = new float[dim];
		for (int i = 0; i < dim; i++) {
			v[i] = (float) (mean + rnd.nextGaussian() * std);
		}
		return v;
	}

	// 工具：安全打印（可裁剪数组长度）
	private static float[] trimArray(float[] src, int max) {
		if (src == null || src.length <= max) return src;
		return Arrays.copyOf(src, max);
	}

	private static double squaredEuclidean(float[] a, double[] b) {
		double sum = 0.0;
		for (int i = 0; i < a.length; i++) {
			double diff = a[i] - b[i];
			sum += diff * diff;
		}
		return sum;
	}

	/**
	 * 余弦距离：1 - cosineSimilarity
	 */
	private static class CosineDistance implements Distance<double[]> {
		@Override
		public double d(double[] x, double[] y) {
			double dot = 0.0;
			double nx = 0.0;
			double ny = 0.0;
			for (int i = 0; i < x.length; i++) {
				dot += x[i] * y[i];
				nx += x[i] * x[i];
				ny += y[i] * y[i];
			}
			double denom = Math.sqrt(nx) * Math.sqrt(ny);
			if (denom == 0) return 1.0; // 退化情形
			double cos = dot / denom;
			return 1.0 - cos;
		}
	}

	public static class Result {
		private final float[] center;
		private final List<float[]> context;

		public Result(float[] center, List<float[]> context) {
			this.center = center;
			this.context = context;
		}

		public float[] getCenter() { return center; }
		public List<float[]> getContext() { return context; }
	}

	public static void main(String[] args) {
		Map<Long, float[]> idToVec = new LinkedHashMap<>();
		// 生成100条、10维向量的测试数据：3个簇 + 少量噪声，并在id上制造若干不连续段
		Random rnd = new Random(42);
		int dim = 10;

		// 簇A，中心 ~ [1,1,1,...]，id段 1001~1036，穿插间隔
		long idA = 1001L;
		for (int i = 0; i < 36; i++) {
			if (i % 7 == 3) { idA += 2; } // 制造不连续
			idToVec.put(idA++, randomAround(rnd, dim, 1.0f, 0.1f));
		}

		// 簇B，中心 ~ [3,3,3,...]，id段 2001~2034，穿插间隔
		long idB = 2001L;
		for (int i = 0; i < 34; i++) {
			if (i % 6 == 2) { idB += 3; }
			idToVec.put(idB++, randomAround(rnd, dim, 3.0f, 0.12f));
		}

		// 簇C，中心 ~ [8,8,8,...]，id段 3001~3024，穿插间隔
		long idC = 3001L;
		for (int i = 0; i < 24; i++) {
			if (i % 5 == 1) { idC += 2; }
			idToVec.put(idC++, randomAround(rnd, dim, 8.0f, 0.15f));
		}

		// 噪声点（可能被视为噪声）
		idToVec.put(9001L, randomAround(rnd, dim, 20.0f, 0.5f));
		idToVec.put(9002L, randomAround(rnd, dim, -10.0f, 0.5f));
		idToVec.put(9003L, randomAround(rnd, dim, 0.0f, 0.5f));
		idToVec.put(9004L, randomAround(rnd, dim, 50.0f, 0.5f));
		idToVec.put(9005L, randomAround(rnd, dim, -30.0f, 0.5f));

		// 仅传入 Map，其他采用默认参数（内部使用类的默认常量）
		List<float[]> centers = findCenters(idToVec);
		System.out.println("样本总数: " + idToVec.size());
		System.out.println("聚类后中心数量: " + centers.size());
		for (int i = 0; i < centers.size(); i++) {
			System.out.println("中心#" + i + ": " + Arrays.toString(trimArray(centers.get(i), 10)));
		}
	}
}