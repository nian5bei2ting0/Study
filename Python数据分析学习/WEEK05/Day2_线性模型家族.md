# Day 2 · 线性模型家族

> 今日目标：掌握线性回归、岭回归、Lasso、逻辑回归、SVM 的原理与使用场景。
> 预计时间：2 小时

---

## 一、线性模型总览

线性模型都用 `y = w₁x₁ + w₂x₂ + ... + b` 的形式，区别在于：
- 损失函数不同（OLS / 正则化 / Hinge / Log）
- 输出不同（连续值 / 概率 / 类别）

| 模型 | 任务 | 损失 | 特点 |
|------|------|------|------|
| 线性回归 | 回归 | OLS | 无正则，易过拟合 |
| 岭回归 Ridge | 回归 | OLS + L2 | 系数压缩，保留所有特征 |
| Lasso | 回归 | OLS + L1 | 系数稀疏，自动选特征 |
| ElasticNet | 回归 | L1 + L2 | 折中 |
| 逻辑回归 | 分类 | Log loss | 输出概率，线性边界 |
| SVM（线性） | 分类 | Hinge | 最大间隔 |

---

## 二、线性回归（最小二乘）

```python
from sklearn.linear_model import LinearRegression
from sklearn.datasets import fetch_california_housing
from sklearn.model_selection import train_test_split

X, y = fetch_california_housing(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)

lr = LinearRegression()
lr.fit(X_tr, y_tr)
print("R²:", lr.score(X_te, y_te))
print("系数:", lr.coef_)
```

**缺点**：无正则，特征多或多重共线时系数爆炸。

---

## 三、岭回归 Ridge（L2 正则）

```python
from sklearn.linear_model import Ridge

ridge = Ridge(alpha=1.0)   # alpha 越大正则越强
ridge.fit(X_tr, y_tr)
print("R²:", ridge.score(X_te, y_te))
```

**L2 正则**：损失 = OLS + α·Σwᵢ²

效果：所有系数被压缩但**不为 0**，保留全部特征。适合**特征都有用但有共线**的场景。

### alpha 调参

```python
for a in [0.01, 0.1, 1, 10, 100]:
    print(a, Ridge(alpha=a).fit(X_tr, y_tr).score(X_te, y_te))
```

---

## 四、Lasso（L1 正则）

```python
from sklearn.linear_model import Lasso

lasso = Lasso(alpha=0.1)
lasso.fit(X_tr, y_tr)
print("R²:", lasso.score(X_te, y_te))
print("非零系数个数:", (lasso.coef_ != 0).sum())   # 自动选特征
```

**L1 正则**：损失 = OLS + α·Σ|wᵢ|

效果：部分系数**直接变 0**，自动做特征选择。适合**特征多但只有少数有用**的场景。

---

## 五、Ridge vs Lasso 怎么选

| 场景 | 选 |
|------|-----|
| 特征都有用，可能共线 | Ridge |
| 特征多，怀疑很多没用 | Lasso |
| 不确定 | ElasticNet（L1+L2） |
| 想要稀疏解（可解释） | Lasso |

---

## 六、逻辑回归（分类）

```python
from sklearn.linear_model import LogisticRegression
from sklearn.datasets import load_breast_cancer

X, y = load_breast_cancer(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

lr = LogisticRegression(C=1.0, max_iter=5000)
lr.fit(X_tr, y_tr)
print("准确率:", lr.score(X_te, y_te))
print("概率:", lr.predict_proba(X_te[:3]))
```

### 关键参数
- `C`：正则化强度的**倒数**，越小正则越强（与 Ridge 的 alpha 相反）
- `penalty`：l1/l2/none
- `class_weight`：处理类别不平衡（`"balanced"`）

### 处理不平衡

```python
lr = LogisticRegression(class_weight="balanced")
```

---

## 七、支持向量机 SVM

```python
from sklearn.svm import SVC, LinearSVC

# 线性 SVM（大样本快）
clf = LinearSVC(C=1.0)
clf.fit(X_tr, y_tr)

# 核 SVM（非线性边界）
clf = SVC(kernel="rbf", C=1.0, gamma="scale")
clf.fit(X_tr, y_tr)
```

### 核函数

| kernel | 边界 | 适用 |
|--------|------|------|
| linear | 线性 | 高维稀疏（文本） |
| rbf | 非线性 | 通用 |
| poly | 多项式 | 特定结构 |

### C 与 gamma
- C 大：少容忍误分（过拟合风险）
- gamma 大：影响范围小（过拟合风险）

> SVM 在中小数据集表现好，**大样本慢**，工业界常用树模型替代。

---

## 八、线性模型优缺点

**优点**
- 训练快、预测快
- 可解释（系数即特征重要性）
- 适合高维稀疏数据（文本、one-hot）

**缺点**
- 只能学线性关系（需手动加多项式/分箱）
- 对异常值敏感
- 假设特征独立

---

## 九、多项式特征扩展非线性

```python
from sklearn.preprocessing import PolynomialFeatures
from sklearn.pipeline import Pipeline

pipe = Pipeline([
    ("poly", PolynomialFeatures(degree=2)),
    ("lr", LinearRegression()),
])
pipe.fit(X_tr, y_tr)
print("带多项式 R²:", pipe.score(X_te, y_te))
```

> degree 太高会过拟合且特征数爆炸，常用 2~3。

---

## 每日练习

用 `fetch_california_housing`：

1. 训练线性回归、岭（alpha=1）、Lasso（alpha=0.1），对比 R²。
2. 调 Ridge 的 alpha 从 0.01 到 100，画 R² 曲线。
3. 看 Lasso 选了几个非零系数。
4. 用逻辑回归在 `load_breast_cancer` 上训练，加 `class_weight="balanced"`，对比准确率与召回率。

---

<details>
<summary>参考答案</summary>

```python
from sklearn.datasets import fetch_california_housing, load_breast_cancer
from sklearn.linear_model import LinearRegression, Ridge, Lasso, LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report
import matplotlib.pyplot as plt

X, y = fetch_california_housing(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)

# 1
for name, m in [("LR", LinearRegression()), ("Ridge", Ridge(1.0)), ("Lasso", Lasso(0.1))]:
    m.fit(X_tr, y_tr)
    print(f"{name}: {m.score(X_te, y_te):.4f}")

# 2
alphas = [0.01, 0.1, 1, 10, 100]
scores = [Ridge(a).fit(X_tr, y_tr).score(X_te, y_te) for a in alphas]
plt.plot(alphas, scores, marker="o"); plt.xscale("log"); plt.show()

# 3
l = Lasso(0.1).fit(X_tr, y_tr)
print("非零系数:", (l.coef_ != 0).sum())

# 4
X, y = load_breast_cancer(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)
for cw in [None, "balanced"]:
    m = LogisticRegression(class_weight=cw, max_iter=5000).fit(X_tr, y_tr)
    print(f"class_weight={cw}")
    print(classification_report(y_te, m.predict(X_te)))
```

</details>

---

## 今日小结

- ✅ 线性回归：无正则，易过拟合
- ✅ Ridge：L2 压缩系数，保留特征
- ✅ Lasso：L1 稀疏，自动选特征
- ✅ 逻辑回归：分类，C 是正则倒数
- ✅ SVM：最大间隔，核技巧处理非线性
- ✅ 多项式特征扩展非线性能力
