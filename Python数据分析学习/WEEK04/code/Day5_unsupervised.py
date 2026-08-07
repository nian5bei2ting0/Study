# Day5 示例：无监督学习（聚类与降维）
# 运行：python code/Day5_unsupervised.py
from sklearn.datasets import make_blobs, make_moons, load_iris
from sklearn.cluster import KMeans, DBSCAN, AgglomerativeClustering
from sklearn.decomposition import PCA
from sklearn.metrics import silhouette_score
from sklearn.ensemble import IsolationForest
import matplotlib.pyplot as plt

plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False

# KMeans
X, _ = make_blobs(n_samples=300, centers=4, random_state=42, cluster_std=0.8)
km = KMeans(n_clusters=4, random_state=42, n_init=10)
labels = km.fit_predict(X)
plt.scatter(X[:,0], X[:,1], c=labels, cmap="viridis", s=20)
plt.scatter(km.cluster_centers_[:,0], km.cluster_centers_[:,1], c="red", marker="X", s=200)
plt.title("KMeans"); plt.show()

# 肘部法则
inertias = [KMeans(k, random_state=42, n_init=10).fit(X).inertia_ for k in range(1, 11)]
plt.plot(range(1,11), inertias, marker="o")
plt.xlabel("K"); plt.ylabel("inertia"); plt.title("肘部法则"); plt.show()

# DBSCAN 月牙
X, _ = make_moons(300, noise=0.05, random_state=42)
fig, axes = plt.subplots(1, 2, figsize=(12,4))
axes[0].scatter(X[:,0], X[:,1], c=KMeans(2, random_state=42, n_init=10).fit_predict(X))
axes[0].set_title("KMeans（月牙失败）")
axes[1].scatter(X[:,0], X[:,1], c=DBSCAN(eps=0.2).fit_predict(X))
axes[1].set_title("DBSCAN（月牙成功）")
plt.show()

# 轮廓系数选 K
X, _ = make_blobs(n_samples=300, centers=4, random_state=42)
for k in range(2, 7):
    print(f"k={k}: {silhouette_score(X, KMeans(k, random_state=42, n_init=10).fit_predict(X)):.4f}")

# PCA 鸢尾花
X, y = load_iris(return_X_y=True)
pca = PCA(n_components=2)
X_pca = pca.fit_transform(X)
print("\n解释方差比:", pca.explained_variance_ratio_)
print("累计:", pca.explained_variance_ratio_.sum())

plt.scatter(X_pca[:,0], X_pca[:,1], c=y, cmap="viridis", s=20)
plt.xlabel("PC1"); plt.ylabel("PC2"); plt.title("PCA 可视化"); plt.show()

# PCA 选维度
pca_full = PCA().fit(X)
plt.plot(range(1, len(pca_full.explained_variance_ratio_)+1),
         pca_full.explained_variance_ratio_.cumsum(), marker="o")
plt.axhline(0.95, color="red", linestyle="--")
plt.xlabel("主成分数"); plt.ylabel("累计方差比"); plt.title("PCA 选维度"); plt.show()

# 异常检测
import numpy as np
X = np.random.randn(200, 2)
X[0] = [10, 10]
iso = IsolationForest(contamination=0.05, random_state=42).fit(X)
labels = iso.predict(X)
plt.scatter(X[:,0], X[:,1], c=labels, cmap="coolwarm", s=20)
plt.title("IsolationForest 异常检测"); plt.show()
print("异常点数:", (labels == -1).sum())
