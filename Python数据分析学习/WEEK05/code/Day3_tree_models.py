# Day3 示例：树模型
# 运行：python code/Day3_tree_models.py
from sklearn.datasets import load_iris, load_breast_cancer, fetch_california_housing
from sklearn.tree import DecisionTreeClassifier, plot_tree, DecisionTreeRegressor
from sklearn.ensemble import (RandomForestClassifier, RandomForestRegressor,
                               GradientBoostingClassifier, HistGradientBoostingClassifier)
from sklearn.inspection import permutation_importance
from sklearn.model_selection import train_test_split
import pandas as pd, time
import matplotlib.pyplot as plt

X, y = load_iris(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

# 决策树
tree = DecisionTreeClassifier(max_depth=3, random_state=42).fit(X_tr, y_tr)
print("Tree:", tree.score(X_te, y_te))
plt.figure(figsize=(12, 8))
plot_tree(tree, feature_names=load_iris().feature_names,
          class_names=load_iris().target_names, filled=True)
plt.show()

# 随机森林
rf = RandomForestClassifier(n_estimators=100, max_depth=8, random_state=42, n_jobs=-1).fit(X_tr, y_tr)
print("RF:", rf.score(X_te, y_te))

# 特征重要性
imp = pd.DataFrame({"f": load_iris().feature_names, "i": rf.feature_importances_})
imp = imp.sort_values("i", ascending=False)
print(imp)
imp.plot.bar(x="f", y="i"); plt.title("特征重要性"); plt.show()

# 排列重要性
result = permutation_importance(rf, X_te, y_te, n_repeats=10, random_state=42)
print("排列重要性:", result.importances_mean)

# GBDT vs HistGBDT
X, y = load_breast_cancer(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)
for name, M in [("GBDT", GradientBoostingClassifier(random_state=42)),
                ("HistGBDT", HistGradientBoostingClassifier(random_state=42))]:
    t = time.time()
    m = M.fit(X_tr, y_tr)
    print(f"{name}: {m.score(X_te, y_te):.4f} 用时{time.time()-t:.3f}s")

# 过拟合演示
overfit = DecisionTreeClassifier(max_depth=None, random_state=42).fit(X_tr, y_tr)
print(f"无限制: train={overfit.score(X_tr, y_tr):.4f} test={overfit.score(X_te, y_te):.4f}")

# 回归树
X, y = fetch_california_housing(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)
rf_r = RandomForestRegressor(n_estimators=100, random_state=42, n_jobs=-1).fit(X_tr, y_tr)
print("RF 回归 R²:", rf_r.score(X_te, y_te))
