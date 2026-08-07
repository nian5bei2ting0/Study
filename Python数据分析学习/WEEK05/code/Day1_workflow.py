# Day1 示例：ML 基础与 sklearn 工作流
# 运行：python code/Day1_workflow.py
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split, cross_val_score, learning_curve
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.neighbors import KNeighborsClassifier
from sklearn.metrics import classification_report
import numpy as np, matplotlib.pyplot as plt

X, y = load_iris(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

# 完整工作流
pipe = Pipeline([("sc", StandardScaler()), ("clf", KNeighborsClassifier(5))])
pipe.fit(X_tr, y_tr)
print("测试准确率:", pipe.score(X_te, y_te))
print(classification_report(y_te, pipe.predict(X_te)))
print("CV:", cross_val_score(pipe, X, y, cv=5).mean())

# 数据泄露对比
scaler = StandardScaler().fit(X)   # 泄露
X_s = scaler.transform(X)
X_tr2, X_te2, y_tr2, y_te2 = train_test_split(X_s, y, test_size=0.2, random_state=42, stratify=y)
print("泄露评估:", KNeighborsClassifier(5).fit(X_tr2, y_tr2).score(X_te2, y_te2))

# K 调参诊断
for k in [1, 5, 20]:
    m = Pipeline([("sc", StandardScaler()), ("clf", KNeighborsClassifier(k))]).fit(X_tr, y_tr)
    print(f"k={k} train={m.score(X_tr, y_tr):.4f} test={m.score(X_te, y_te):.4f}")

# 学习曲线
ts, tr_s, te_s = learning_curve(pipe, X, y, cv=5, train_sizes=np.linspace(0.1, 1.0, 10))
plt.plot(ts, tr_s.mean(axis=1), label="train")
plt.plot(ts, te_s.mean(axis=1), label="test")
plt.xlabel("样本数"); plt.legend(); plt.title("学习曲线"); plt.show()
