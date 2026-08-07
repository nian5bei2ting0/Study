# Day 3 · 监督学习：分类

> 今日目标：理解并跑通 KNN、逻辑回归、决策树三个经典分类算法，学会评估指标。
> 预计时间：2 小时

---

## 一、分类问题概述

**分类**：预测离散类别标签。

| 类型 | 例子 |
|------|------|
| 二分类 | 垃圾邮件（是/否）、疾病（有/无） |
| 多分类 | 鸢尾花品种（3类）、手写数字（10类） |

今天学三个最经典的分类算法：

| 算法 | 思想 | 优点 | 缺点 |
|------|------|------|------|
| **KNN** | 看最近的 K 个邻居 | 简单直观 | 慢，需缩放 |
| **逻辑回归** | 线性决策边界 | 快、可解释 | 只能线性 |
| **决策树** | 一系列 if-else | 可解释、不需缩放 | 易过拟合 |

---

## 二、KNN：K近邻

**思想**：一个样本的类别由离它最近的 K 个邻居投票决定。

```python
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
from sklearn.neighbors import KNeighborsClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline

X, y = load_iris(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

pipe = Pipeline([
    ("sc", StandardScaler()),
    ("knn", KNeighborsClassifier(n_neighbors=5)),
])
pipe.fit(X_tr, y_tr)
print("KNN 准确率:", pipe.score(X_te, y_te))
```

### K 的选择
- K 太小（如 1）：过拟合，对噪声敏感
- K 太大：欠拟合，决策边界太粗
- 经验：K 取奇数（避免平票），通常 3~10

> ⚠️ KNN 必须缩放，否则数值大的特征主导距离。

---

## 三、逻辑回归 Logistic Regression

**思想**：用 sigmoid 函数把线性输出压到 0~1，表示"属于正类的概率"。

```python
from sklearn.linear_model import LogisticRegression

model = LogisticRegression(max_iter=200)
model.fit(X_tr, y_tr)
print("逻辑回归准确率:", model.score(X_te, y_te))
print("系数:", model.coef_)   # 每个特征的权重
```

### 关键参数
- `C`：正则化强度的倒数，越小正则越强（防过拟合）
- `max_iter`：最大迭代次数，不收敛时调大
- `penalty`：正则类型（l1/l2/none）

### 二分类阈值

```python
# 预测概率
proba = model.predict_proba(X_te)
print("前5个样本属于各类的概率:\n", proba[:5])
# 默认阈值 0.5，可自定义
```

> 逻辑回归虽带"回归"二字，**实际是分类模型**。

---

## 四、决策树 Decision Tree

**思想**：学一棵 if-else 树，每个节点按某特征分裂。

```python
from sklearn.tree import DecisionTreeClassifier, plot_tree
import matplotlib.pyplot as plt

tree = DecisionTreeClassifier(max_depth=3, random_state=42)
tree.fit(X_tr, y_tr)
print("决策树准确率:", tree.score(X_te, y_te))

# 可视化树
plt.figure(figsize=(12, 8))
plot_tree(tree, feature_names=load_iris().feature_names,
          class_names=load_iris().target_names, filled=True)
plt.show()
```

### 关键参数（控制复杂度防过拟合）
- `max_depth`：树最大深度
- `min_samples_split`：节点继续分裂所需最小样本数
- `min_samples_leaf`：叶子节点最小样本数
- `max_features`：每次分裂考虑的特征数

### 特征重要性

```python
print("特征重要性:", tree.feature_importances_)
```

> 决策树**不需要缩放**，因为它是按阈值分裂，与量纲无关。

---

## 五、分类评估指标（重点！）

光看准确率不够，尤其在**类别不平衡**时（如 99% 是负样本，全猜负也能 99%）。

### 1. 混淆矩阵

```python
from sklearn.metrics import confusion_matrix, ConfusionMatrixDisplay
y_pred = pipe.predict(X_te)
cm = confusion_matrix(y_te, y_pred)
print(cm)
ConfusionMatrixDisplay(cm, display_labels=load_iris().target_names).plot()
plt.show()
```

二分类混淆矩阵：

```
              预测正  预测负
真实正  →  TP      FN
真实负  →  FP      TN
```

- TP 真阳、FN 假阴、FP 假阳、TN 真阴

### 2. 准确率 Accuracy

```python
from sklearn.metrics import accuracy_score
print("准确率:", accuracy_score(y_te, y_pred))
```

= (TP+TN) / 总数。**类别均衡时用**。

### 3. 精确率 Precision

```python
from sklearn.metrics import precision_score
print("精确率:", precision_score(y_te, y_pred, average="macro"))
```

= TP / (TP+FP)。**预测为正的里，真的是正的比例**。
- 适用：垃圾邮件（误杀正常邮件代价高，要高 Precision）

### 4. 召回率 Recall

```python
from sklearn.metrics import recall_score
print("召回率:", recall_score(y_te, y_pred, average="macro"))
```

= TP / (TP+FN)。**真的是正的里，被找出来的比例**。
- 适用：疾病筛查（漏诊代价高，要高 Recall）

### 5. F1 分数

```python
from sklearn.metrics import f1_score
print("F1:", f1_score(y_te, y_pred, average="macro"))
```

= 2·P·R / (P+R)。精确率和召回率的调和平均，**类别不平衡时用**。

### 6. 分类报告（一次出全）

```python
from sklearn.metrics import classification_report
print(classification_report(y_te, y_pred, target_names=load_iris().target_names))
```

### 多分类的 average 参数

| average | 含义 |
|---------|------|
| `macro` | 各类指标平均（类别平等） |
| `micro` | 全局算（小类被大类淹没） |
| `weighted` | 按样本数加权 |

---

## 六、指标怎么选

| 场景 | 主看指标 |
|------|---------|
| 类别均衡 | accuracy |
| 类别不平衡 | F1 / recall / precision |
| 关注漏报（疾病/欺诈） | recall |
| 关注误报（垃圾邮件） | precision |

---

## 七、三模型对比

```python
from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier
from sklearn.neighbors import KNeighborsClassifier

models = {
    "KNN": Pipeline([("sc", StandardScaler()), ("m", KNeighborsClassifier(5))]),
    "LogReg": LogisticRegression(max_iter=200),
    "Tree": DecisionTreeClassifier(max_depth=3, random_state=42),
}

for name, m in models.items():
    m.fit(X_tr, y_tr)
    print(f"{name}: {m.score(X_te, y_te):.4f}")
```

---

## 每日练习

用 `load_breast_cancer`（乳腺癌二分类）完成：

1. 划分 8:2 训练测试集，分别用 KNN、逻辑回归、决策树训练，对比准确率。
2. 输出逻辑回归的混淆矩阵和分类报告。
3. 思考：癌症筛查应该更看重 Precision 还是 Recall？为什么？
4. 调决策树 `max_depth` 从 1 到 10，画训练集和测试集准确率曲线，找过拟合拐点。

---

<details>
<summary>参考答案</summary>

```python
from sklearn.datasets import load_breast_cancer
from sklearn.model_selection import train_test_split
from sklearn.neighbors import KNeighborsClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.metrics import classification_report, confusion_matrix
import matplotlib.pyplot as plt

X, y = load_breast_cancer(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

# 1
models = {
    "KNN": Pipeline([("sc", StandardScaler()), ("m", KNeighborsClassifier(5))]),
    "LogReg": Pipeline([("sc", StandardScaler()), ("m", LogisticRegression(max_iter=5000))]),
    "Tree": DecisionTreeClassifier(max_depth=4, random_state=42),
}
for name, m in models.items():
    m.fit(X_tr, y_tr)
    print(f"{name}: {m.score(X_te, y_te):.4f}")

# 2
y_pred = models["LogReg"].predict(X_te)
print(confusion_matrix(y_te, y_pred))
print(classification_report(y_te, y_pred, target_names=load_breast_cancer().target_names))

# 3: 癌症筛查重 Recall（漏诊代价大）

# 4
train_scores, test_scores = [], []
for d in range(1, 11):
    t = DecisionTreeClassifier(max_depth=d, random_state=42).fit(X_tr, y_tr)
    train_scores.append(t.score(X_tr, y_tr))
    test_scores.append(t.score(X_te, y_te))
plt.plot(range(1,11), train_scores, label="train")
plt.plot(range(1,11), test_scores, label="test")
plt.xlabel("max_depth"); plt.ylabel("accuracy"); plt.legend()
plt.show()
```

</details>

---

## 今日小结

- ✅ KNN：看邻居投票，需缩放，K 取奇数 3~10
- ✅ 逻辑回归：线性 + sigmoid，输出概率
- ✅ 决策树：if-else 树，可解释，需剪枝防过拟合
- ✅ 混淆矩阵：TP/FN/FP/TN
- ✅ 指标：accuracy / precision / recall / F1
- ✅ 不平衡看 F1，关注漏报看 recall，关注误报看 precision

明天学：回归与更系统的模型评估。
