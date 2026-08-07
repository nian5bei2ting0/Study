# Day 3 · 树模型：决策树 / 随机森林 / GBDT

> 今日目标：掌握树模型家族，理解集成学习，能用特征重要性做分析。
> 预计时间：2 小时

---

## 一、决策树 Decision Tree

**思想**：学一棵 if-else 树，每次按某特征分裂，使子节点纯度提升。

### 分类树

```python
from sklearn.tree import DecisionTreeClassifier, plot_tree
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
import matplotlib.pyplot as plt

X, y = load_iris(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

tree = DecisionTreeClassifier(max_depth=3, random_state=42)
tree.fit(X_tr, y_tr)
print("准确率:", tree.score(X_te, y_te))

plt.figure(figsize=(12, 8))
plot_tree(tree, feature_names=load_iris().feature_names,
          class_names=load_iris().target_names, filled=True)
plt.show()
```

### 分裂准则

| 准则 | 含义 | 适用 |
|------|------|------|
| gini | 基尼不纯度（默认） | 分类 |
| entropy | 信息增益 | 分类 |
| mse | 均方误差 | 回归 |

### 关键参数（控制复杂度）

- `max_depth`：树最大深度（最常用）
- `min_samples_split`：分裂所需最小样本数
- `min_samples_leaf`：叶子最小样本数
- `max_features`：每次分裂考虑的特征数

### 优缺点

- ✅ 可解释、不需缩放、能处理非线性
- ❌ 易过拟合、不稳定（数据小改树大变）

---

## 二、集成学习思想

> 单棵树易过拟合，**多棵树组合**更稳更准。

| 类型 | 思想 | 代表 |
|------|------|------|
| Bagging | 并行建多棵树，投票 | 随机森林 |
| Boosting | 串行建树，后一棵纠正前一棵 | GBDT / XGBoost |

---

## 三、随机森林 Random Forest

**思想**：Bagging + 随机特征选择。每棵树看不同数据子集 + 不同特征子集，最后投票。

```python
from sklearn.ensemble import RandomForestClassifier

rf = RandomForestClassifier(n_estimators=100, max_depth=8, random_state=42, n_jobs=-1)
rf.fit(X_tr, y_tr)
print("准确率:", rf.score(X_te, y_te))
print("特征重要性:", rf.feature_importances_)
```

### 关键参数
- `n_estimators`：树数（越多越稳但越慢，100~500 常用）
- `max_depth`：每棵树深度
- `max_features`：每棵树考虑的特征数（"sqrt" 默认）
- `n_jobs`：并行数（-1 用全部 CPU）

### 优点
- 抗过拟合（多树平均）
- 能估计特征重要性
- 几乎不需调参就不错
- **工业界基线模型首选**

---

## 四、GBDT 梯度提升树

**思想**：串行建树，每棵树拟合前一棵的**残差**（预测错误）。

```python
from sklearn.ensemble import GradientBoostingClassifier

gbdt = GradientBoostingClassifier(n_estimators=100, learning_rate=0.1, max_depth=3, random_state=42)
gbdt.fit(X_tr, y_tr)
print("准确率:", gbdt.score(X_te, y_te))
```

### 关键参数
- `n_estimators`：树数
- `learning_rate`：学习率（小则需更多树，但更稳）
- `max_depth`：通常 3~5（浅树防过拟合）

### GBDT vs 随机森林

| 维度 | 随机森林 | GBDT |
|------|---------|------|
| 方式 | 并行 | 串行 |
| 目标 | 降方差 | 降偏差 |
| 过拟合风险 | 低 | 较高（需早停） |
| 调参难度 | 低 | 中 |
| 精度 | 好 | 通常更好 |

---

## 五、Histogram GBDT（推荐）

sklearn 的 `HistGradientBoostingClassifier` 速度比传统 GBDT 快 10 倍+，支持缺失值：

```python
from sklearn.ensemble import HistGradientBoostingClassifier

hgb = HistGradientBoostingClassifier(max_iter=100, learning_rate=0.1, random_state=42)
hgb.fit(X_tr, y_tr)
print("准确率:", hgb.score(X_te, y_te))
```

> 大数据集首选，性能接近 XGBoost/LightGBM。

---

## 六、特征重要性

```python
import pandas as pd
import numpy as np

importances = rf.feature_importances_
feat_df = pd.DataFrame({
    "feature": load_iris().feature_names,
    "importance": importances,
}).sort_values("importance", ascending=False)
print(feat_df)

feat_df.plot.bar(x="feature", y="importance")
plt.title("特征重要性"); plt.show()
```

### 排列重要性（更可靠）

```python
from sklearn.inspection import permutation_importance
result = permutation_importance(rf, X_te, y_te, n_repeats=10, random_state=42)
print(result.importances_mean)
```

> 树自带的 importance 偏向高基数特征，排列重要性更客观。

---

## 七、回归树

```python
from sklearn.ensemble import RandomForestRegressor
from sklearn.datasets import fetch_california_housing

X, y = fetch_california_housing(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)

rf = RandomForestRegressor(n_estimators=100, random_state=42, n_jobs=-1)
rf.fit(X_tr, y_tr)
print("R²:", rf.score(X_te, y_te))
```

---

## 八、树模型优缺点总结

**优点**
- 能学非线性关系
- 不需缩放
- 能处理混合类型特征
- 给特征重要性
- 对异常值鲁棒

**缺点**
- 可解释性不如线性模型
- 大数据训练慢（GBDT 尤甚）
- 高维稀疏数据不如线性
- 外推能力差（不能预测训练范围外）

---

## 每日练习

用 `load_breast_cancer`：

1. 训练单棵决策树（max_depth=3）和随机森林（100 棵），对比准确率。
2. 输出随机森林的特征重要性 Top 5，画条形图。
3. 训练 GBDT 和 HistGBDT，对比准确率和训练时间。
4. 故意把 max_depth 设为 None（不限制），看决策树是否过拟合。

---

<details>
<summary>参考答案</summary>

```python
from sklearn.datasets import load_breast_cancer
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier, HistGradientBoostingClassifier
from sklearn.model_selection import train_test_split
import pandas as pd, time
import matplotlib.pyplot as plt

X, y = load_breast_cancer(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

# 1
tree = DecisionTreeClassifier(max_depth=3, random_state=42).fit(X_tr, y_tr)
rf = RandomForestClassifier(n_estimators=100, random_state=42, n_jobs=-1).fit(X_tr, y_tr)
print("Tree:", tree.score(X_te, y_te), "RF:", rf.score(X_te, y_te))

# 2
imp = pd.DataFrame({"f": load_breast_cancer().feature_names, "i": rf.feature_importances_})
imp = imp.sort_values("i", ascending=False).head(5)
print(imp)
imp.plot.bar(x="f", y="i"); plt.show()

# 3
for name, M in [("GBDT", GradientBoostingClassifier(random_state=42)),
                ("HistGBDT", HistGradientBoostingClassifier(random_state=42))]:
    t = time.time()
    m = M.fit(X_tr, y_tr)
    print(f"{name}: {m.score(X_te, y_te):.4f} 用时{time.time()-t:.3f}s")

# 4
overfit = DecisionTreeClassifier(max_depth=None, random_state=42).fit(X_tr, y_tr)
print("无限制 train:", overfit.score(X_tr, y_tr), "test:", overfit.score(X_te, y_te))
```

</details>

---

## 今日小结

- ✅ 决策树：if-else 树，可解释，易过拟合
- ✅ 随机森林：Bagging 降方差，工业界基线
- ✅ GBDT：Boosting 降偏差，精度高
- ✅ HistGBDT：快速版，大数据首选
- ✅ 特征重要性：树自带 + 排列重要性
- ✅ Bagging vs Boosting 的本质区别
