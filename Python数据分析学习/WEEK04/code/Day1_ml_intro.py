# Day1 示例：ML 全景与 sklearn 入门
# 运行：python code/Day1_ml_intro.py
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
from sklearn.neighbors import KNeighborsClassifier
from sklearn.metrics import accuracy_score

# 加载数据
iris = load_iris()
X, y = iris.data, iris.target
print("特征:", iris.feature_names)
print("标签:", iris.target_names)
print("X:", X.shape, "y:", y.shape)

# 划分训练/测试集
X_tr, X_te, y_tr, y_te = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)
print("训练集:", X_tr.shape, "测试集:", X_te.shape)

# 创建并训练模型
model = KNeighborsClassifier(n_neighbors=3)
model.fit(X_tr, y_tr)

# 预测与评估
y_pred = model.predict(X_te)
print("准确率:", accuracy_score(y_te, y_pred))
print("模型 score:", model.score(X_te, y_te))

# 不同 k 值对比
print("\n不同 k 值准确率:")
for k in [1, 3, 5, 10, 20]:
    m = KNeighborsClassifier(n_neighbors=k).fit(X_tr, y_tr)
    print(f"  k={k}: {m.score(X_te, y_te):.4f}")

# 错误示范：用全部数据训练再评估
m = KNeighborsClassifier(n_neighbors=3).fit(X, y)
print("\n作弊评估（用全部数据）:", m.score(X, y))
