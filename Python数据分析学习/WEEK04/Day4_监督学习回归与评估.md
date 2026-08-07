# Day 4 · 监督学习：回归与模型评估

> 今日目标：掌握线性回归、决策树回归，学会回归评估指标与交叉验证。
> 预计时间：2 小时

---

## 一、回归问题概述

**回归**：预测连续数值。如房价、销量、温度。

| 算法 | 思想 |
|------|------|
| **线性回归** | 找一条直线/超平面拟合数据 |
| **决策树回归** | 在叶子节点取均值 |
| **随机森林回归** | 多棵树平均（Day5 顺带提） |

---

## 二、线性回归

**思想**：找 y = w₁x₁ + w₂x₂ + ... + b，让预测值和真实值误差最小（最小二乘）。

```python
from sklearn.datasets import fetch_california_housing
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LinearRegression
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline

X, y = fetch_california_housing(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)

pipe = Pipeline([("sc", StandardScaler()), ("lr", LinearRegression())])
pipe.fit(X_tr, y_tr)
print("系数:", pipe.named_steps["lr"].coef_)
print("截距:", pipe.named_steps["lr"].intercept_)
print("R²:", pipe.score(X_te, y_te))
```

### R² 决定系数

= 1 - (残差平方和 / 总平方和)。
- R²=1：完美预测
- R²=0：等于直接预测均值
- R²<0：比均值还差

> R² 是回归的默认 `score`，越接近 1 越好。

---

## 三、决策树回归

```python
from sklearn.tree import DecisionTreeRegressor

tree = DecisionTreeRegressor(max_depth=5, random_state=42)
tree.fit(X_tr, y_tr)
print("决策树 R²:", tree.score(X_te, y_te))
print("特征重要性:", tree.feature_importances_)
```

决策树能拟合非线性关系，但易过拟合。

---

## 四、回归评估指标

### 1. MAE 平均绝对误差

```python
from sklearn.metrics import mean_absolute_error
y_pred = pipe.predict(X_te)
print("MAE:", mean_absolute_error(y_te, y_pred))
```

= 平均 |真实-预测|。**单位和 y 相同**，直观。

### 2. MSE 均方误差

```python
from sklearn.metrics import mean_squared_error
print("MSE:", mean_squared_error(y_te, y_pred))
```

= 平均 (真实-预测)²。**对大误差敏感**（平方放大），但单位是 y²。

### 3. RMSE 均方根误差

```python
import numpy as np
print("RMSE:", np.sqrt(mean_squared_error(y_te, y_pred)))
```

= √MSE。**单位和 y 相同**，最常用。

### 4. R²

```python
from sklearn.metrics import r2_score
print("R²:", r2_score(y_te, y_pred))
```

### 指标对比

| 指标 | 单位 | 特点 |
|------|------|------|
| MAE | y | 直观，抗异常 |
| MSE | y² | 对大误差敏感 |
| RMSE | y | 直观 + 对大误差敏感 |
| R² | 无量纲 | 0~1，跨问题可比 |

> 实战常用：**RMSE + R²** 一起看。

---

## 五、交叉验证 Cross-Validation

单次划分训练测试集结果波动大。K 折交叉验证更稳：

```
数据：[==][==][==][==][==]
第1折：测  训  训  训  训
第2折：训  测  训  训  训
...
5 次结果取平均
```

```python
from sklearn.model_selection import cross_val_score
scores = cross_val_score(pipe, X, y, cv=5, scoring="r2")
print("5折 R²:", scores)
print("平均:", scores.mean(), "标准差:", scores.std())
```

> `cv=5` 表示 5 折。`scoring` 可换 "neg_mean_squared_error" 等。

### 常用 scoring

| 任务 | scoring |
|------|---------|
| 分类 | accuracy / f1_macro / roc_auc |
| 回归 | r2 / neg_mean_squared_error / neg_mean_absolute_error |

> ⚠️ 回归的 MSE 在 sklearn 里是**负值**（因为优化方向是越大越好），用 `np.sqrt(-score)` 还原。

---

## 六、网格搜索 GridSearchCV

自动遍历参数组合找最优：

```python
from sklearn.model_selection import GridSearchCV
from sklearn.neighbors import KNeighborsRegressor

param_grid = {"n_neighbors": [3, 5, 7, 10, 15]}
gs = GridSearchCV(KNeighborsRegressor(), param_grid, cv=5, scoring="r2")
gs.fit(X_tr, y_tr)
print("最佳参数:", gs.best_params_)
print("最佳 CV 分数:", gs.best_score_)
print("测试集 R²:", gs.score(X_te, y_te))
```

### 多参数组合

```python
param_grid = {
    "n_neighbors": [3, 5, 7],
    "weights": ["uniform", "distance"],
}
gs = GridSearchCV(KNeighborsRegressor(), param_grid, cv=5)
gs.fit(X_tr, y_tr)
print(gs.best_params_, gs.best_score_)
```

> 参数组合多时 GridSearchCV 会慢，可改用 `RandomizedSearchCV` 随机采样。

---

## 七、学习曲线：诊断过/欠拟合

```python
from sklearn.model_selection import learning_curve
import numpy as np
import matplotlib.pyplot as plt

train_sizes, train_scores, test_scores = learning_curve(
    pipe, X, y, cv=5, train_sizes=np.linspace(0.1, 1.0, 10), scoring="r2"
)
train_mean = train_scores.mean(axis=1)
test_mean = test_scores.mean(axis=1)

plt.plot(train_sizes, train_mean, label="train")
plt.plot(train_sizes, test_mean, label="test")
plt.xlabel("样本数"); plt.ylabel("R²"); plt.legend()
plt.title("学习曲线")
plt.show()
```

读法：
- 两条线都低 → 欠拟合（加复杂模型/加特征）
- 训练高测试低 → 过拟合（加数据/简化模型/正则）
- 两条线接近且高 → 刚好

---

## 八、模型对比

```python
from sklearn.linear_model import LinearRegression, Ridge
from sklearn.tree import DecisionTreeRegressor
from sklearn.ensemble import RandomForestRegressor

models = {
    "LinearRegression": Pipeline([("sc", StandardScaler()), ("m", LinearRegression())]),
    "Ridge": Pipeline([("sc", StandardScaler()), ("m", Ridge(alpha=1.0))]),
    "DecisionTree": DecisionTreeRegressor(max_depth=5, random_state=42),
    "RandomForest": RandomForestRegressor(n_estimators=50, random_state=42),
}

for name, m in models.items():
    scores = cross_val_score(m, X, y, cv=5, scoring="r2")
    print(f"{name}: R²={scores.mean():.4f} (±{scores.std():.4f})")
```

---

## 每日练习

用 `fetch_california_housing` 完成：

1. 训练线性回归，输出 R²、MAE、RMSE。
2. 用 5 折交叉验证评估线性回归，输出均值±标准差。
3. 用 GridSearchCV 找决策树回归的最佳 `max_depth`（候选 3/5/7/10/None）。
4. 画学习曲线，判断模型是过拟合还是欠拟合。

---

<details>
<summary>参考答案</summary>

```python
from sklearn.datasets import fetch_california_housing
from sklearn.model_selection import train_test_split, cross_val_score, GridSearchCV, learning_curve
from sklearn.linear_model import LinearRegression
from sklearn.tree import DecisionTreeRegressor
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
import numpy as np, matplotlib.pyplot as plt

X, y = fetch_california_housing(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)

# 1
pipe = Pipeline([("sc", StandardScaler()), ("lr", LinearRegression())])
pipe.fit(X_tr, y_tr)
y_pred = pipe.predict(X_te)
print("R²:", r2_score(y_te, y_pred))
print("MAE:", mean_absolute_error(y_te, y_pred))
print("RMSE:", np.sqrt(mean_squared_error(y_te, y_pred)))

# 2
scores = cross_val_score(pipe, X, y, cv=5, scoring="r2")
print(f"CV: {scores.mean():.4f} ± {scores.std():.4f}")

# 3
gs = GridSearchCV(DecisionTreeRegressor(random_state=42),
                  {"max_depth": [3,5,7,10,None]}, cv=5, scoring="r2")
gs.fit(X_tr, y_tr)
print("最佳:", gs.best_params_, gs.best_score_)

# 4
train_sizes, tr_s, te_s = learning_curve(pipe, X, y, cv=5, scoring="r2", train_sizes=np.linspace(0.1,1.0,10))
plt.plot(train_sizes, tr_s.mean(axis=1), label="train")
plt.plot(train_sizes, te_s.mean(axis=1), label="test")
plt.legend(); plt.show()
```

</details>

---

## 今日小结

- ✅ 线性回归：最小二乘，R² 评估
- ✅ 决策树回归：非线性，易过拟合
- ✅ 回归指标：MAE / MSE / RMSE / R²
- ✅ 交叉验证：K 折更稳定
- ✅ GridSearchCV 自动调参
- ✅ 学习曲线诊断过/欠拟合

明天学：无监督学习——聚类与降维。
