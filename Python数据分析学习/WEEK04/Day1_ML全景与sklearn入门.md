# Day 1 · 机器学习全景与 scikit-learn 入门

> 今日目标：建立机器学习的整体认知，跑通第一个 sklearn 模型。
> 预计时间：2 小时

---

## 一、什么是机器学习

**机器学习（Machine Learning, ML）**：让计算机从数据中**自动学习规律**，而不是被人显式编程规则。

经典定义（Tom Mitchell）：如果一个程序在任务 T 上的性能 P 随经验 E 的增加而提高，就说它从经验 E 中学习。

举例：
- 传统编程：人写规则"金额 > 10万 且 深夜 = 风险"
- 机器学习：给模型大量历史交易 + 是否欺诈的标签，让它自己学出规则

---

## 二、机器学习三大类

| 类型 | 数据特点 | 任务 | 例子 |
|------|---------|------|------|
| **监督学习** | 有标签 | 预测标签 | 垃圾邮件识别、房价预测 |
| **无监督学习** | 无标签 | 找结构 | 客户分群、降维 |
| **强化学习** | 有奖励 | 学策略 | 游戏AI、机器人 |

本周重点：监督学习（Day3-4）+ 无监督学习（Day5）。强化学习了解即可。

### 监督学习再分

| 子类 | 标签类型 | 例子 |
|------|---------|------|
| **分类** | 离散类别 | 邮件是不是垃圾（是/否） |
| **回归** | 连续数值 | 房价预测（具体金额） |

---

## 三、机器学习工作流（核心！）

```
1. 数据准备 → 2. 特征工程 → 3. 划分训练/测试集
                                          ↓
6. 部署应用 ← 5. 调参优化 ← 4. 训练模型 + 评估
```

**关键原则**：测试集不能参与训练，否则评估会"作弊"。

---

## 四、scikit-learn 简介

Python 最经典的机器学习库，特点：
- 统一 API：所有模型都是 `fit / predict / transform`
- 文档优秀，社区活跃
- 适合入门和中小项目（深度学习用 PyTorch/TensorFlow）

约定俗成：`from sklearn.xxx import yyy`。

### 统一 API（记住这四步，所有模型通用）

```python
model = SomeModel()        # 1. 创建模型
model.fit(X_train, y_train) # 2. 训练
y_pred = model.predict(X_test)  # 3. 预测
score = model.score(X_test, y_test)  # 4. 评估
```

> 这四步是 sklearn 的灵魂，**学会它就掌握了 80%**。

---

## 五、第一个模型：鸢尾花分类

经典入门数据集：根据花萼/花瓣的长宽，预测鸢尾花品种（3 类）。

```python
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
from sklearn.neighbors import KNeighborsClassifier
from sklearn.metrics import accuracy_score

# 1. 加载数据
iris = load_iris()
X, y = iris.data, iris.target          # X 特征，y 标签
print("特征:", iris.feature_names)
print("标签:", iris.target_names)
print("X 形状:", X.shape, "y 形状:", y.shape)

# 2. 划分训练/测试集（8:2）
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)
print("训练集:", X_train.shape, "测试集:", X_test.shape)

# 3. 创建并训练模型（KNN：K近邻）
model = KNeighborsClassifier(n_neighbors=3)
model.fit(X_train, y_train)

# 4. 预测与评估
y_pred = model.predict(X_test)
print("准确率:", accuracy_score(y_test, y_pred))
print("模型自带 score:", model.score(X_test, y_test))
```

### 关键概念

- **X**：特征矩阵（二维数组，行=样本，列=特征）
- **y**：标签向量（一维数组）
- **训练集**：模型学习用
- **测试集**：评估用，**模型没见过**
- **random_state**：随机种子，保证结果可复现
- **stratify**：分层抽样，保持各类别比例

---

## 六、看懂数据：EDA 先行

建模前一定要先看数据（WEEK03 学的）：

```python
import pandas as pd
df = pd.DataFrame(iris.data, columns=iris.feature_names)
df["species"] = [iris.target_names[i] for i in iris.target]
print(df.describe())
print(df["species"].value_counts())

import seaborn as sns
sns.pairplot(df, hue="species")
```

> **跳过 EDA 直接建模 = 灾难**。永远先看数据。

---

## 七、训练集 vs 测试集为什么重要

如果用全部数据训练再用全部数据评估：

```python
# 错误示范
model.fit(X, y)
print(model.score(X, y))   # 0.97 看起来很好
```

这叫**训练集准确率**，模型可能只是"记住"了数据，没真正学到规律。遇到新数据就崩。

正确做法是划分训练/测试集，**测试集是模型没见过的"新数据"**，评估才真实。

---

## 八、过拟合与欠拟合

| 现象 | 表现 | 原因 |
|------|------|------|
| **欠拟合** | 训练集也差 | 模型太简单 |
| **过拟合** | 训练集好、测试集差 | 模型太复杂，记住噪声 |
| **刚好** | 两者都不错且接近 | 模型复杂度合适 |

```
欠拟合 ── 刚好 ── 过拟合
模型太简单 ──────── 模型太复杂
```

判断方法：**对比训练集和测试集分数**。差距大 = 过拟合。

---

## 九、sklearn 数据集速览

```python
from sklearn import datasets

# 玩具数据集（小，内置）
iris = datasets.load_iris()              # 分类
digits = datasets.load_digits()          # 手写数字分类
boston = datasets.fetch_california_housing()  # 回归（加州房价）
breast_cancer = datasets.load_breast_cancer()  # 二分类

# 造数据（用于练习）
from sklearn.datasets import make_classification, make_blobs
X, y = make_classification(n_samples=1000, n_features=10, random_state=42)
```

---

## 每日练习

1. 加载 `load_iris`，划分 7:3 训练测试集，用 KNN（k=5）训练，输出测试集准确率。
2. 改 `n_neighbors` 为 1、3、5、10、20，看准确率怎么变，思考为什么。
3. 加载 `load_digits`（手写数字），用 KNN 训练，输出准确率。
4. 故意用全部数据训练再评估全部数据，对比和正确做法的差距。

---

<details>
<summary>参考答案</summary>

**练习 1**

```python
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
from sklearn.neighbors import KNeighborsClassifier
from sklearn.metrics import accuracy_score

X, y = load_iris(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.3, random_state=42, stratify=y)
model = KNeighborsClassifier(n_neighbors=5)
model.fit(X_tr, y_tr)
print("准确率:", accuracy_score(y_te, model.predict(X_te)))
```

**练习 2**

```python
for k in [1, 3, 5, 10, 20]:
    m = KNeighborsClassifier(n_neighbors=k)
    m.fit(X_tr, y_tr)
    print(f"k={k}: {m.score(X_te, y_te):.4f}")
# k 太小易过拟合，太大易欠拟合，通常 3~10 较好
```

**练习 3**

```python
from sklearn.datasets import load_digits
X, y = load_digits(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.3, random_state=42)
KNeighborsClassifier(n_neighbors=3).fit(X_tr, y_tr).score(X_te, y_te)
```

**练习 4**

```python
m = KNeighborsClassifier(n_neighbors=3)
m.fit(X, y)
print("作弊评估:", m.score(X, y))   # 会偏高
```

</details>

---

## 今日小结

- ✅ ML 三大类：监督 / 无监督 / 强化
- ✅ 监督学习分分类（离散）和回归（连续）
- ✅ 工作流：数据→特征→划分→训练→评估→调参
- ✅ sklearn 统一 API：fit / predict / score
- ✅ 训练集 vs 测试集，测试集不能参与训练
- ✅ 过拟合（训练好测试差）vs 欠拟合（都差）

明天学：数据预处理与特征工程——模型好不好，80% 取决于数据。
