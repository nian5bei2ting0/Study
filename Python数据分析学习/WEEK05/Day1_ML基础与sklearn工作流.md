# Day 1 · ML 基础回顾与 sklearn 工作流

> 今日目标：巩固 ML 核心概念，深入理解 sklearn 的统一 API 与工作流。
> 预计时间：2 小时

---

## 一、ML 核心概念回顾

### 1. 监督学习 vs 无监督学习

| 类型 | 数据 | 目标 | 算法 |
|------|------|------|------|
| 监督 | 有标签 | 预测标签 | 分类/回归 |
| 无监督 | 无标签 | 找结构 | 聚类/降维 |

### 2. 分类 vs 回归

- 分类：标签离散（垃圾邮件是/否）
- 回归：标签连续（房价数值）

### 3. 训练集 vs 测试集

- 训练集：模型学习用
- 测试集：评估用，**模型没见过**
- 验证集：调参用（小数据集可用交叉验证代替）

---

## 二、偏差-方差权衡（核心！）

模型误差可分解为：

```
总误差 = 偏差² + 方差 + 不可约误差
```

| 项 | 含义 | 高的表现 |
|----|------|---------|
| **偏差 Bias** | 模型假设太简单 | 训练集也差（欠拟合） |
| **方差 Variance** | 模型对训练数据太敏感 | 训练好测试差（过拟合） |
| 不可约误差 | 数据本身的噪声 | 无法消除 |

### 诊断表

| 训练误差 | 测试误差 | 诊断 | 对策 |
|---------|---------|------|------|
| 高 | 高 | 欠拟合（高偏差） | 加复杂模型/加特征 |
| 低 | 高 | 过拟合（高方差） | 加数据/简化模型/正则 |
| 低 | 低 | 刚好 | 维持 |

> 这是 ML 调参的核心指南针，**所有调参都是为了找偏差-方差的甜点**。

---

## 三、sklearn 统一 API（深入）

所有 estimator 都遵循同一接口：

```python
model = Estimator(hyperparams)   # 1. 实例化（带超参）
model.fit(X, y)                  # 2. 训练
model.predict(X_new)             # 3. 预测
model.score(X, y)                # 4. 评估（分类=acc，回归=R²）
model.transform(X)               # 预处理器：转换数据
model.fit_transform(X)           # 拟合+转换
```

### 估算器类型

| 类型 | 方法 | 例子 |
|------|------|------|
| 估算器 Estimator | fit | 所有模型 |
| 预测器 Predictor | predict | 监督模型 |
| 转换器 Transformer | transform | StandardScaler / PCA |

---

## 四、完整工作流模板

```python
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.neighbors import KNeighborsClassifier
from sklearn.metrics import classification_report

# 1. 数据
X, y = load_iris(return_X_y=True)

# 2. 划分
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2,
                                            random_state=42, stratify=y)

# 3. Pipeline（预处理+模型）
pipe = Pipeline([
    ("sc", StandardScaler()),
    ("clf", KNeighborsClassifier(n_neighbors=5)),
])

# 4. 训练
pipe.fit(X_tr, y_tr)

# 5. 评估
y_pred = pipe.predict(X_te)
print(classification_report(y_te, y_pred))

# 6. 交叉验证（更稳）
scores = cross_val_score(pipe, X, y, cv=5)
print(f"CV: {scores.mean():.4f} ± {scores.std():.4f}")
```

---

## 五、random_state 与可复现性

```python
# 不设 → 每次结果不同
# 设了 → 每次结果相同
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)
model = RandomForestClassifier(random_state=42)
```

> 调试和对比时**必须设 random_state**，否则无法判断改进是真实还是随机。

---

## 六、数据泄露防范

**最常见陷阱**：在划分前对全数据做预处理，导致测试集信息泄露到训练参数里。

```python
# 错误：先 fit scaler 再划分
scaler.fit(X)              # 见过全部数据
X_tr, X_te = train_test_split(X)

# 正确：先划分再 fit
X_tr, X_te = train_test_split(X)
scaler.fit(X_tr)           # 只见训练集
X_tr_s = scaler.transform(X_tr)
X_te_s = scaler.transform(X_te)
```

> **Pipeline 自动防止泄露**，强烈推荐用 Pipeline 而非手动分步。

---

## 七、sklearn 配置对象 set_config

```python
from sklearn import set_config
set_config(display="diagram")   # Jupyter 里显示 Pipeline 流程图
set_config(transform_output="pandas")  # transform 返回 DataFrame（1.2+）
```

---

## 每日练习

1. 用 `load_breast_cancer`，写完整工作流（Pipeline + 5 折 CV），输出准确率。
2. 故意制造数据泄露（先 fit scaler 再划分），对比正确做法的评估差异。
3. 对 KNN 调 k=1/5/20，记录训练和测试准确率，判断哪个过拟合哪个欠拟合。
4. 用学习曲线判断练习 1 的模型是否过拟合。

---

<details>
<summary>参考答案</summary>

```python
from sklearn.datasets import load_breast_cancer
from sklearn.model_selection import train_test_split, cross_val_score, learning_curve
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.neighbors import KNeighborsClassifier
import matplotlib.pyplot as plt
import numpy as np

X, y = load_breast_cancer(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

# 1
pipe = Pipeline([("sc", StandardScaler()), ("clf", KNeighborsClassifier(5))])
pipe.fit(X_tr, y_tr)
print("测试准确率:", pipe.score(X_te, y_te))
print("CV:", cross_val_score(pipe, X, y, cv=5).mean())

# 2 数据泄露
scaler = StandardScaler().fit(X)   # 泄露
X_s = scaler.transform(X)
X_tr2, X_te2, y_tr2, y_te2 = train_test_split(X_s, y, test_size=0.2, random_state=42, stratify=y)
print("泄露评估:", KNeighborsClassifier(5).fit(X_tr2, y_tr2).score(X_te2, y_te2))

# 3
for k in [1, 5, 20]:
    m = Pipeline([("sc", StandardScaler()), ("clf", KNeighborsClassifier(k))]).fit(X_tr, y_tr)
    print(f"k={k} train={m.score(X_tr, y_tr):.4f} test={m.score(X_te, y_te):.4f}")

# 4
ts, tr_s, te_s = learning_curve(pipe, X, y, cv=5, train_sizes=np.linspace(0.1, 1.0, 10))
plt.plot(ts, tr_s.mean(axis=1), label="train")
plt.plot(ts, te_s.mean(axis=1), label="test")
plt.legend(); plt.show()
```

</details>

---

## 今日小结

- ✅ 偏差-方差权衡是调参指南针
- ✅ sklearn 统一 API：fit / predict / transform
- ✅ 完整工作流：数据→划分→Pipeline→训练→评估→CV
- ✅ random_state 保证可复现
- ✅ Pipeline 防止数据泄露
