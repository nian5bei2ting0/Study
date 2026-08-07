# Day 5 · 无监督学习：聚类与降维

> 今日目标：掌握 KMeans/DBSCAN 聚类和 PCA 降维，能在无标签数据中找结构。
> 预计时间：2 小时

---

## 一、无监督学习概述

**无监督学习**：数据**没有标签**，让算法自己找结构。

| 任务 | 目的 | 算法 |
|------|------|------|
| **聚类** | 把相似样本分组 | KMeans / DBSCAN / 层次聚类 |
| **降维** | 压缩特征、可视化 | PCA / t-SNE / UMAP |
| **异常检测** | 找离群点 | IsolationForest |

应用：客户分群、文章主题发现、图像压缩、数据可视化。

---

## 二、KMeans 聚类

**思想**：指定 K 个簇，迭代地把每个样本分给最近的簇中心，再更新簇中心。

```python
from sklearn.datasets import make_blobs
from sklearn.cluster import KMeans
import matplotlib.pyplot as plt

X, _ = make_blobs(n_samples=300, centers=4, random_state=42, cluster_std=0.8)

kmeans = KMeans(n_clusters=4, random_state=42, n_init=10)
labels = kmeans.fit_predict(X)
print("簇中心:\n", kmeans.cluster_centers_)

plt.scatter(X[:,0], X[:,1], c=labels, cmap="viridis", s=20)
plt.scatter(kmeans.cluster_centers_[:,0], kmeans.cluster_centers_[:,1],
            c="red", marker="X", s=200)
plt.title("KMeans 聚类")
plt.show()
```

### 关键参数
- `n_clusters`：簇数（必须指定）
- `n_init`：重复初始化次数（默认 10，取最优）
- `random_state`：可复现

### K 怎么选：肘部法则

```python
inertias = []
for k in range(1, 11):
    km = KMeans(n_clusters=k, random_state=42, n_init=10).fit(X)
    inertias.append(km.inertia_)   # 簇内平方和

plt.plot(range(1,11), inertias, marker="o")
plt.xlabel("K"); plt.ylabel("inertia"); plt.title("肘部法则")
plt.show()
```

找"肘部"——inertia 下降变缓的拐点，就是合适的 K。

### KMeans 局限
- 需要预先指定 K
- 假设簇是球形、大小相近
- 对异常值敏感
- 处理不了非凸形状（如环形）

---

## 三、DBSCAN 密度聚类

**思想**：在密集区域连成簇，稀疏点算噪声。**不用指定簇数**，能识别任意形状。

```python
from sklearn.cluster import DBSCAN

X, _ = make_moons(n_samples=300, noise=0.05, random_state=42)   # 月牙形

db = DBSCAN(eps=0.2, min_samples=5)
labels = db.fit_predict(X)
print("簇数:", len(set(labels)) - (1 if -1 in labels else 0))
print("噪声点数:", (labels == -1).sum())

plt.scatter(X[:,0], X[:,1], c=labels, cmap="viridis", s=20)
plt.title("DBSCAN 聚类（-1 是噪声）")
plt.show()
```

### 关键参数
- `eps`：邻域半径（最重要）
- `min_samples`：核心点所需最小邻居数

> DBSCAN 适合不规则形状、有噪声的数据；KMeans 适合球形簇。

---

## 四、聚类评估（无标签怎么评？）

### 1. 轮廓系数 Silhouette

```python
from sklearn.metrics import silhouette_score
labels = KMeans(n_clusters=4, random_state=42, n_init=10).fit_predict(X)
print("轮廓系数:", silhouette_score(X, labels))
```

- 范围 [-1, 1]
- 接近 1：簇内紧凑、簇间分离好
- 接近 0：簇重叠
- 接近 -1：分错

### 2. 用轮廓系数选 K

```python
for k in range(2, 8):
    labels = KMeans(n_clusters=k, random_state=42, n_init=10).fit_predict(X)
    print(f"k={k}: {silhouette_score(X, labels):.4f}")
```

> 轮廓系数最大的 K 通常最优（前提是数据真有簇结构）。

---

## 五、层次聚类

```python
from sklearn.cluster import AgglomerativeClustering

agg = AgglomerativeClustering(n_clusters=4, linkage="ward")
labels = agg.fit_predict(X)
```

不指定 K 也能用，画**树状图 dendrogram** 看聚类过程（需 scipy）。

---

## 六、PCA 降维

**主成分分析**：把高维数据投影到低维，保留最多方差（信息）。

```python
from sklearn.decomposition import PCA
from sklearn.datasets import load_iris

X, y = load_iris(return_X_y=True)   # 4 维

pca = PCA(n_components=2)           # 降到 2 维
X_pca = pca.fit_transform(X)
print("降维后:", X_pca.shape)
print("各主成分解释方差比:", pca.explained_variance_ratio_)
print("累计:", pca.explained_variance_ratio_.sum())
```

### 解释方差比

```python
pca_full = PCA().fit(X)
plt.plot(range(1, len(pca_full.explained_variance_ratio_)+1),
         pca_full.explained_variance_ratio_.cumsum(), marker="o")
plt.axhline(0.95, color="red", linestyle="--")
plt.xlabel("主成分数"); plt.ylabel("累计方差比")
plt.title("PCA 选择维度")
plt.show()
```

> 通常保留 95% 方差。鸢尾花前 2 个主成分就能解释 ~97.8%。

### PCA 可视化

```python
plt.scatter(X_pca[:,0], X_pca[:,1], c=y, cmap="viridis", s=20)
plt.xlabel("PC1"); plt.ylabel("PC2")
plt.title("PCA 降到 2D 可视化")
plt.show()
```

> PCA 最常见用途：**把高维数据降到 2D/3D 画图**。

---

## 七、PCA + KMeans 组合实战

```python
from sklearn.datasets import make_blobs
from sklearn.cluster import KMeans
from sklearn.decomposition import PCA

X, _ = make_blobs(n_samples=500, centers=5, n_features=10, random_state=42)
# 10 维数据，先聚类再降维可视化

labels = KMeans(n_clusters=5, random_state=42, n_init=10).fit_predict(X)
X_2d = PCA(n_components=2).fit_transform(X)

plt.scatter(X_2d[:,0], X_2d[:,1], c=labels, cmap="viridis", s=20)
plt.title("高维数据聚类结果（PCA 可视化）")
plt.show()
```

---

## 八、异常检测 IsolationForest

```python
from sklearn.ensemble import IsolationForest
import numpy as np

X = np.random.randn(200, 2)
X[0] = [10, 10]   # 造一个异常点

iso = IsolationForest(contamination=0.05, random_state=42)
labels = iso.fit_predict(X)   # 1=正常, -1=异常
print("异常点数:", (labels == -1).sum())

plt.scatter(X[:,0], X[:,1], c=labels, cmap="coolwarm", s=20)
plt.title("IsolationForest 异常检测")
plt.show()
```

---

## 每日练习

1. 用 `make_blobs` 造 4 簇数据，KMeans 聚类并画图，用肘部法则验证 K=4。
2. 用 `make_moons` 造月牙数据，分别用 KMeans 和 DBSCAN 聚类，对比效果。
3. 用轮廓系数选鸢尾花数据集的最佳 K（2~6）。
4. 把鸢尾花 4 维数据用 PCA 降到 2 维画散点图，标注真实类别颜色。

---

<details>
<summary>参考答案</summary>

```python
from sklearn.datasets import make_blobs, make_moons, load_iris
from sklearn.cluster import KMeans, DBSCAN
from sklearn.decomposition import PCA
from sklearn.metrics import silhouette_score
import matplotlib.pyplot as plt

# 1
X, _ = make_blobs(n_samples=300, centers=4, random_state=42)
inertias = [KMeans(k, random_state=42, n_init=10).fit(X).inertia_ for k in range(1,11)]
plt.plot(range(1,11), inertias, marker="o"); plt.title("肘部"); plt.show()
labels = KMeans(4, random_state=42, n_init=10).fit_predict(X)
plt.scatter(X[:,0], X[:,1], c=labels); plt.title("KMeans"); plt.show()

# 2
X, _ = make_moons(300, noise=0.05, random_state=42)
fig, axes = plt.subplots(1, 2, figsize=(12,4))
axes[0].scatter(X[:,0], X[:,1], c=KMeans(2, random_state=42, n_init=10).fit_predict(X))
axes[0].set_title("KMeans（月牙失败）")
axes[1].scatter(X[:,0], X[:,1], c=DBSCAN(eps=0.2).fit_predict(X))
axes[1].set_title("DBSCAN（月牙成功）")
plt.show()

# 3
X, y = load_iris(return_X_y=True)
for k in range(2, 7):
    print(f"k={k}: {silhouette_score(X, KMeans(k, random_state=42, n_init=10).fit_predict(X)):.4f}")

# 4
X_pca = PCA(n_components=2).fit_transform(X)
plt.scatter(X_pca[:,0], X_pca[:,1], c=y, cmap="viridis", s=20)
plt.xlabel("PC1"); plt.ylabel("PC2"); plt.title("PCA 可视化")
plt.show()
```

</details>

---

## 今日小结

- ✅ KMeans：需指定 K，球形簇，肘部法则选 K
- ✅ DBSCAN：密度聚类，不需 K，能识别任意形状+噪声
- ✅ 轮廓系数评估聚类质量
- ✅ PCA 降维：保留最大方差，常用于可视化
- ✅ 解释方差比选维度（通常保留 95%）
- ✅ IsolationForest 异常检测

明天进入 AI 全景图：从 ML 到 LLM 的认知地图。
