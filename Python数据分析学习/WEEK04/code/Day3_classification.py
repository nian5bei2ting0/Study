# Day3 示例：监督学习-分类
# 运行：python code/Day3_classification.py
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
from sklearn.neighbors import KNeighborsClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier, plot_tree
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.metrics import (accuracy_score, classification_report,
                             confusion_matrix, ConfusionMatrixDisplay)
import matplotlib.pyplot as plt

plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False

X, y = load_iris(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

# KNN
knn = Pipeline([("sc", StandardScaler()), ("m", KNeighborsClassifier(n_neighbors=5))])
knn.fit(X_tr, y_tr)
print("KNN:", knn.score(X_te, y_te))

# 逻辑回归
lr = Pipeline([("sc", StandardScaler()), ("m", LogisticRegression(max_iter=200))])
lr.fit(X_tr, y_tr)
print("LogReg:", lr.score(X_te, y_te))
print("系数:", lr.named_steps["m"].coef_)
print("概率示例:", lr.predict_proba(X_te[:3]))

# 决策树
tree = DecisionTreeClassifier(max_depth=3, random_state=42).fit(X_tr, y_tr)
print("Tree:", tree.score(X_te, y_te))
print("特征重要性:", tree.feature_importances_)

# 可视化树
plt.figure(figsize=(12, 8))
plot_tree(tree, feature_names=load_iris().feature_names,
          class_names=load_iris().target_names, filled=True)
plt.title("决策树"); plt.show()

# 评估指标
y_pred = lr.predict(X_te)
print("\n准确率:", accuracy_score(y_te, y_pred))
print("\n分类报告:")
print(classification_report(y_te, y_pred, target_names=load_iris().target_names))

cm = confusion_matrix(y_te, y_pred)
print("\n混淆矩阵:\n", cm)
ConfusionMatrixDisplay(cm, display_labels=load_iris().target_names).plot()
plt.title("混淆矩阵"); plt.show()

# 三模型对比
print("\n三模型对比:")
for name, m in [("KNN", knn), ("LogReg", lr), ("Tree", tree)]:
    print(f"  {name}: {m.score(X_te, y_te):.4f}")
