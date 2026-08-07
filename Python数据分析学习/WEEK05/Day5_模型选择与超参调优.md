# Day 5 · 模型选择与超参调优

> 今日目标：掌握 GridSearchCV、RandomizedSearchCV，学会用 Pipeline 做规范调参。
> 预计时间：2 小时

---

## 一、为什么需要调参

模型默认参数是"通用值"，**不是最优值**。每个数据集的最佳参数不同，必须搜索。

| 模型 | 关键超参 | 影响 |
|------|---------|------|
| KNN | n_neighbors | 复杂度 |
| 决策树 | max_depth | 复杂度 |
| 随机森林 | n_estimators, max_depth | 复杂度 |
| Ridge | alpha | 正则强度 |
| 逻辑回归 | C | 正则强度 |
| SVM | C, gamma | 边界 |

---

## 二、GridSearchCV 网格搜索

**穷举**所有参数组合，CV 评估选最优。

```python
from sklearn.model_selection import GridSearchCV
from sklearn.ensemble import RandomForestClassifier
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split

X, y = load_iris(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

param_grid = {
    "n_estimators": [50, 100, 200],
    "max_depth": [3, 5, 8, None],
    "min_samples_split": [2, 5],
}
gs = GridSearchCV(RandomForestClassifier(random_state=42),
                  param_grid, cv=5, scoring="accuracy", n_jobs=-1)
gs.fit(X_tr, y_tr)
print("最佳参数:", gs.best_params_)
print("最佳 CV 分数:", gs.best_score_)
print("测试集:", gs.score(X_te, y_te))
```

### 组合数计算

`3 × 4 × 2 × 5(CV) = 120` 次拟合。组合多时**很慢**。

---

## 三、RandomizedSearchCV 随机搜索

**随机采样**参数组合，比网格快，常能找到接近最优的解。

```python
from sklearn.model_selection import RandomizedSearchCV
from scipy.stats import randint

param_dist = {
    "n_estimators": randint(50, 300),
    "max_depth": [3, 5, 8, None],
    "min_samples_split": randint(2, 10),
}
rs = RandomizedSearchCV(RandomForestClassifier(random_state=42),
                        param_dist, n_iter=20, cv=5, scoring="accuracy",
                        random_state=42, n_jobs=-1)
rs.fit(X_tr, y_tr)
print("最佳:", rs.best_params_, rs.best_score_)
```

### Grid vs Random

| 维度 | Grid | Random |
|------|------|--------|
| 方式 | 穷举 | 随机采样 |
| 速度 | 慢 | 快 |
| 精度 | 找到最优 | 接近最优 |
| 适用 | 参数少 | 参数多/连续 |

> 经验：参数多或连续时用 Random，参数少且离散时用 Grid。

---

## 四、连续参数用分布

```python
from scipy.stats import uniform, loguniform

param_dist = {
    "C": loguniform(1e-3, 1e3),     # 对数均匀（适合正则强度）
    "gamma": loguniform(1e-5, 1e1),
}
```

- `loguniform`：对数均匀，适合跨多个数量级的参数（如 C、alpha、learning_rate）
- `uniform`：线性均匀
- `randint`：整数范围

---

## 五、Pipeline 中调参（重点！）

调参时参数名要写"步骤名__参数名"：

```python
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.neighbors import KNeighborsClassifier

pipe = Pipeline([
    ("sc", StandardScaler()),
    ("knn", KNeighborsClassifier()),
])

param_grid = {
    "knn__n_neighbors": [3, 5, 7, 10],
    "knn__weights": ["uniform", "distance"],
}
gs = GridSearchCV(pipe, param_grid, cv=5)
gs.fit(X_tr, y_tr)
print(gs.best_params_)
```

> 注意双下划线 `knn__n_neighbors`，对应 Pipeline 里步骤名 `knn`。

---

## 六、在预处理参数上调

```python
param_grid = {
    "poly__degree": [2, 3],            # 多项式阶数
    "model__alpha": [0.01, 0.1, 1.0], # 正则强度
}
pipe = Pipeline([("poly", PolynomialFeatures()), ("model", Ridge())])
gs = GridSearchCV(pipe, param_grid, cv=5)
```

> Pipeline + GridSearch 是 sklearn 工程化的**最佳实践**，自动防泄露 + 一次调好。

---

## 七、早停与迭代调参

GBDT 类模型可设 `n_iter_no_change` 早停：

```python
from sklearn.ensemble import HistGradientBoostingClassifier

hgb = HistGradientBoostingClassifier(max_iter=500, early_stopping=True,
                                      n_iter_no_change=10, random_state=42)
hgb.fit(X_tr, y_tr)
print("实际迭代:", hgb.n_iter_)
```

> 早停能自动找到最佳迭代数，避免过拟合。

---

## 八、模型选择对比

```python
from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier
from sklearn.naive_bayes import GaussianNB
from sklearn.neighbors import KNeighborsClassifier
from sklearn.model_selection import cross_val_score
import pandas as pd

models = {
    "LogReg": Pipeline([("sc", StandardScaler()), ("m", LogisticRegression(max_iter=5000))]),
    "Tree": DecisionTreeClassifier(max_depth=5, random_state=42),
    "RF": RandomForestClassifier(100, random_state=42, n_jobs=-1),
    "NB": GaussianNB(),
    "KNN": Pipeline([("sc", StandardScaler()), ("m", KNeighborsClassifier(5))]),
}

results = []
for name, m in models.items():
    s = cross_val_score(m, X, y, cv=5)
    results.append({"model": name, "mean": s.mean(), "std": s.std()})
print(pd.DataFrame(results).sort_values("mean", ascending=False))
```

---

## 九、调参经验法则

1. **先粗调后细调**：大范围 Random 找区域，小范围 Grid 精调
2. **优先调关键参数**：max_depth、n_estimators、C/alpha
3. **看 CV 标准差**：std 大说明不稳定，需加数据或简化
4. **别过度调参**：测试集提升 0.5% 可能是噪声
5. **对比基线**：调完要和默认参数比，确认真的提升

---

## 每日练习

用 `load_breast_cancer`：

1. 用 GridSearchCV 调随机森林的 `n_estimators` 和 `max_depth`。
2. 用 RandomizedSearchCV 调 KNN 的 `n_neighbors` 和 `weights`（Pipeline 内）。
3. 对比 5 个模型的 5 折 CV 准确率，选最好的。
4. 对最佳模型用小范围 GridSearchCV 精调。

---

<details>
<summary>参考答案</summary>

```python
from sklearn.datasets import load_breast_cancer
from sklearn.ensemble import RandomForestClassifier
from sklearn.neighbors import KNeighborsClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier
from sklearn.naive_bayes import GaussianNB
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.model_selection import GridSearchCV, RandomizedSearchCV, cross_val_score
from scipy.stats import randint
import pandas as pd

X, y = load_breast_cancer(return_X_y=True)

# 1
gs = GridSearchCV(RandomForestClassifier(random_state=42),
                  {"n_estimators":[50,100,200], "max_depth":[3,5,8,None]},
                  cv=5, n_jobs=-1).fit(X, y)
print(gs.best_params_, gs.best_score_)

# 2
pipe = Pipeline([("sc", StandardScaler()), ("knn", KNeighborsClassifier())])
rs = RandomizedSearchCV(pipe, {"knn__n_neighbors":randint(1,20),
                                "knn__weights":["uniform","distance"]},
                         n_iter=15, cv=5, random_state=42, n_jobs=-1).fit(X, y)
print(rs.best_params_, rs.best_score_)

# 3
models = {"LogReg":Pipeline([("sc",StandardScaler()),("m",LogisticRegression(max_iter=5000))]),
          "Tree":DecisionTreeClassifier(max_depth=5,random_state=42),
          "RF":RandomForestClassifier(100,random_state=42,n_jobs=-1),
          "NB":GaussianNB(),"KNN":Pipeline([("sc",StandardScaler()),("m",KNeighborsClassifier(5))])}
res = [{"m":n,"mean":cross_val_score(m,X,y,cv=5).mean()} for n,m in models.items()]
print(pd.DataFrame(res).sort_values("mean",ascending=False))

# 4
best = GridSearchCV(RandomForestClassifier(random_state=42),
                    {"n_estimators":[80,100,120],"max_depth":[5,8,10]}, cv=5).fit(X,y)
print(best.best_params_, best.best_score_)
```

</details>

---

## 今日小结

- ✅ GridSearchCV：穷举，参数少时用
- ✅ RandomizedSearchCV：随机，参数多/连续时用
- ✅ 连续参数用 loguniform / uniform / randint
- ✅ Pipeline 调参用 `步骤名__参数名`
- ✅ 早停避免 GBDT 过拟合
- ✅ 先粗后细，对比基线
