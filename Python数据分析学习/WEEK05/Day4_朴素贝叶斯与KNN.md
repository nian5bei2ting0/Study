# Day 4 · 朴素贝叶斯与 KNN

> 今日目标：掌握两类经典算法——基于概率的朴素贝叶斯和基于距离的 KNN。
> 预计时间：2 小时

---

# 第一部分：朴素贝叶斯

## 一、贝叶斯公式回顾

```
P(类别|特征) = P(特征|类别) × P(类别) / P(特征)
```

即：后验 = 似然 × 先验 / 证据。

**朴素假设**：特征之间**条件独立**。这是"朴素"二字的由来，现实中很少成立，但效果常不错。

---

## 二、sklearn 中的朴素贝叶斯

| 类型 | 适用特征 | 例子 |
|------|---------|------|
| GaussianNB | 连续特征（假设高斯） | 鸢尾花 |
| MultinomialNB | 计数特征 | 文本词频 |
| BernoulliNB | 0/1 特征 | 短文本 |
| ComplementNB | 不平衡文本 | 不平衡分类 |

---

## 三、GaussianNB 实战

```python
from sklearn.naive_bayes import GaussianNB
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split

X, y = load_iris(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

gnb = GaussianNB()
gnb.fit(X_tr, y_tr)
print("准确率:", gnb.score(X_te, y_te))
```

> GaussianNB 几乎无超参，**训练极快**，常作基线。

---

## 四、文本分类（MultinomialNB）

```python
from sklearn.naive_bayes import MultinomialNB
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.pipeline import Pipeline

texts = ["免费领取大奖", "明天开会", "限时优惠点击", "项目进度如何",
         "中奖了快来", "周报已发", "免费体验", "会议纪要"]
labels = ["垃圾", "正常", "垃圾", "正常", "垃圾", "正常", "垃圾", "正常"]

pipe = Pipeline([
    ("vec", CountVectorizer()),
    ("clf", MultinomialNB()),
])
pipe.fit(texts, labels)

test = ["免费中奖", "下周一开会"]
print(pipe.predict(test))
```

> 朴素贝叶斯是**文本分类的经典基线**，速度快，小数据表现好。

---

## 五、朴素贝叶斯优缺点

**优点**
- 训练预测极快
- 小数据表现好
- 天然支持多分类
- 输出概率（可调阈值）

**缺点**
- 朴素假设常不成立
- 特征独立时表现差
- 概率值常偏高（排序可用，绝对值不准）

---

# 第二部分：KNN

## 六、KNN 原理

**K 近邻**：一个样本的类别由离它最近的 K 个邻居投票决定。

```
新样本 → 算到所有训练点的距离 → 取最近 K 个 → 投票/平均
```

---

## 七、KNN 分类与回归

```python
from sklearn.neighbors import KNeighborsClassifier, KNeighborsRegressor

# 分类
knn = KNeighborsClassifier(n_neighbors=5, weights="uniform")
knn.fit(X_tr, y_tr)
print("准确率:", knn.score(X_te, y_te))

# 回归
from sklearn.datasets import fetch_california_housing
X, y = fetch_california_housing(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)
knn_r = KNeighborsRegressor(n_neighbors=5, weights="distance")
knn_r.fit(X_tr, y_tr)
print("R²:", knn_r.score(X_te, y_te))
```

---

## 八、关键参数

### 1. n_neighbors（K）

- K 小：复杂，过拟合
- K 大：简单，欠拟合
- 经验：3~10，用交叉验证选

### 2. weights（权重）

- `"uniform"`：等权投票
- `"distance"`：距离倒数加权（近的权重大）

### 3. metric（距离度量）

```python
from sklearn.neighbors import KNeighborsClassifier
knn = KNeighborsClassifier(metric="manhattan")  # 曼哈顿距离
```

| metric | 含义 | 适用 |
|--------|------|------|
| euclidean | 欧氏（默认） | 连续 |
| manhattan | 曼哈顿 | 高维 |
| minkowski | 闵可夫斯基（p 可调） | 通用 |

---

## 九、KNN 必须缩放

```python
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline

pipe = Pipeline([("sc", StandardScaler()), ("knn", KNeighborsClassifier(5))])
pipe.fit(X_tr, y_tr)
print("缩放后:", pipe.score(X_te, y_te))
```

> 不缩放时，量纲大的特征主导距离，KNN 会失效。

---

## 十、KNN 的复杂度

- 训练：O(1)（只是存数据）
- 预测：O(N×D)（每次都要算到所有点距离）

> **大样本预测慢**，可用 `algorithm="kd_tree"` 或 `"ball_tree"` 加速。

---

## 十一、KNN vs 朴素贝叶斯

| 维度 | 朴素贝叶斯 | KNN |
|------|-----------|-----|
| 原理 | 概率 | 距离 |
| 训练 | 快（算统计量） | 极快（存数据） |
| 预测 | 快 | 慢（大样本） |
| 需缩放 | 否 | 是 |
| 可解释 | 概率 | 邻居 |

---

## 每日练习

1. 用 GaussianNB 在 `load_iris` 上训练，对比 KNN（缩放后）的准确率。
2. 用 MultinomialNB 做一个简单的中英文垃圾短信分类（自造 10 条数据）。
3. 调 KNN 的 K 从 1 到 20，画准确率曲线，找最佳 K。
4. 对比 KNN 的 `weights="uniform"` 和 `"distance"`，看哪个好。

---

<details>
<summary>参考答案</summary>

```python
from sklearn.naive_bayes import GaussianNB, MultinomialNB
from sklearn.neighbors import KNeighborsClassifier
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
import matplotlib.pyplot as plt

X, y = load_iris(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

# 1
print("GNB:", GaussianNB().fit(X_tr, y_tr).score(X_te, y_te))
print("KNN:", Pipeline([("sc", StandardScaler()),
                        ("k", KNeighborsClassifier(5))]).fit(X_tr, y_tr).score(X_te, y_te))

# 2
texts = ["免费领奖", "明天开会", "中奖快来", "项目周报", "限时优惠",
         "会议纪要", "免费体验", "下班路上", "点击领取", "晚饭吃啥"]
labels = ["垃圾","正常","垃圾","正常","垃圾","正常","垃圾","正常","垃圾","正常"]
pipe = Pipeline([("v", CountVectorizer()), ("c", MultinomialNB())]).fit(texts, labels)
print(pipe.predict(["免费中奖", "周报"]))

# 3
scores = []
for k in range(1, 21):
    m = Pipeline([("sc", StandardScaler()),
                  ("k", KNeighborsClassifier(k))]).fit(X_tr, y_tr)
    scores.append(m.score(X_te, y_te))
plt.plot(range(1,21), scores, marker="o"); plt.show()

# 4
for w in ["uniform", "distance"]:
    m = Pipeline([("sc", StandardScaler()),
                  ("k", KNeighborsClassifier(5, weights=w))]).fit(X_tr, y_tr)
    print(w, m.score(X_te, y_te))
```

</details>

---

## 今日小结

- ✅ 朴素贝叶斯：贝叶斯公式 + 独立假设
- ✅ GaussianNB（连续）/ MultinomialNB（文本）/ BernoulliNB（0/1）
- ✅ KNN：距离投票，必须缩放
- ✅ K 选择：交叉验证，3~10
- ✅ weights：uniform / distance
- ✅ KNN 训练快预测慢，NB 都快
