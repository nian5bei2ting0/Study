# Day 2 · 数据预处理与特征工程

> 今日目标：掌握 sklearn 的预处理工具与 Pipeline，让建模流程规范可复用。
> 预计时间：2 小时

---

## 一、为什么预处理很重要

> 业内共识：**数据和特征决定了模型上限，算法只是逼近这个上限**。

原始数据通常有：
- 缺失值
- 类别文本（模型只懂数字）
- 量纲差异大（如年龄 0~100，收入 0~1000000）
- 异常值
- 冗余/无关特征

预处理就是把数据变成模型能吃、且吃得好用的形式。

---

## 二、缺失值处理

sklearn 的 `SimpleImputer`：

```python
import numpy as np
from sklearn.impute import SimpleImputer

X = np.array([[1, 2, np.nan], [4, np.nan, 6], [7, 8, 9]])

imp_mean = SimpleImputer(strategy="mean")      # 均值
imp_median = SimpleImputer(strategy="median")  # 中位数
imp_most = SimpleImputer(strategy="most_frequent")  # 众数
imp_const = SimpleImputer(strategy="constant", fill_value=0)  # 常数

print(imp_mean.fit_transform(X))
```

> 与 Pandas 的 `fillna` 等价，但 sklearn 版本能进 Pipeline（后面讲）。

---

## 三、类别编码

模型只懂数字，类别（如"北京/上海/广州"）必须编码。

### 1. 标签编码 LabelEncoder（仅用于标签 y）

```python
from sklearn.preprocessing import LabelEncoder
y = ["猫","狗","鸟","猫"]
le = LabelEncoder()
print(le.fit_transform(y))   # [1 0 2 1]
print(le.classes_)            # ['狗' '猫' '鸟']
print(le.inverse_transform([1,0,2]))  # 还原
```

### 2. 独热编码 OneHotEncoder（用于特征 X）

```python
from sklearn.preprocessing import OneHotEncoder
X = [["北京"],["上海"],["广州"],["北京"]]
ohe = OneHotEncoder(sparse_output=False)   # dense 数组
print(ohe.fit_transform(X))
# [[1 0 0]
#  [0 1 0]
#  [0 0 1]
#  [1 0 0]]
```

> ⚠️ **特征用 OneHot，标签用 Label**。特征用 LabelEncoder 会引入"大小关系"（如广州=2 > 上海=1），误导模型。

### 3. 有序类别用 OrdinalEncoder

```python
from sklearn.preprocessing import OrdinalEncoder
X = [["低"],["中"],["高"],["中"]]
oe = OrdinalEncoder(categories=[["低","中","高"]])
print(oe.fit_transform(X))   # [[0],[2],[1],[1]]  按指定顺序
```

---

## 四、特征缩放

不同特征量纲差太多会让某些模型（如 KNN、SVM、线性回归）偏向数值大的特征。

### 1. 标准化 StandardScaler（z-score，最常用）

```python
from sklearn.preprocessing import StandardScaler
X = np.array([[1, 1000], [2, 2000], [3, 3000]])
scaler = StandardScaler()
print(scaler.fit_transform(X))
# 每列变成均值0、标准差1
```

### 2. 归一化 MinMaxScaler（缩到 0~1）

```python
from sklearn.preprocessing import MinMaxScaler
print(MinMaxScaler().fit_transform(X))
```

### 3. 鲁棒缩放 RobustScaler（抗异常值）

```python
from sklearn.preprocessing import RobustScaler
print(RobustScaler().fit_transform(X))
# 用中位数和四分位数，异常值影响小
```

| 缩放器 | 适用 |
|--------|------|
| StandardScaler | 大多数情况，假设近似正态 |
| MinMaxScaler | 需要固定范围（如神经网络） |
| RobustScaler | 有异常值 |
| 不缩放 | 树模型（决策树/随机森林）不受量纲影响 |

> ⚠️ **树模型不需要缩放**，别画蛇添足。

---

## 五、多项式特征

给线性模型加非线性能力：

```python
from sklearn.preprocessing import PolynomialFeatures
X = np.array([[2], [3], [4]])
poly = PolynomialFeatures(degree=2)
print(poly.fit_transform(X))
# [[1. 2. 4.]   x → 1, x, x²
#  [1. 3. 9.]
#  [1. 4. 16.]]
```

---

## 六、特征选择

去掉无关/冗余特征，提升效果并加快训练。

### 1. 方差阈值（去掉低方差特征）

```python
from sklearn.feature_selection import VarianceThreshold
X = np.array([[0, 1], [0, 2], [0, 3], [0, 4]])
print(VarianceThreshold(threshold=0.0).fit_transform(X))
# 第0列方差0，被去掉
```

### 2. 选 K 个最好特征

```python
from sklearn.feature_selection import SelectKBest, f_classif
from sklearn.datasets import load_iris
X, y = load_iris(return_X_y=True)
X_new = SelectKBest(f_classif, k=2).fit_transform(X, y)
print(X_new.shape)   # (150, 2)
```

### 3. 基于模型的重要性

```python
from sklearn.ensemble import RandomForestClassifier
rf = RandomForestClassifier().fit(X, y)
print(rf.feature_importances_)   # 每个特征的重要性
```

---

## 七、Pipeline：把流程串起来（重点！）

Pipeline 把多步预处理 + 模型串成一个对象，**避免数据泄露**且代码整洁。

```python
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.neighbors import KNeighborsClassifier
from sklearn.model_selection import train_test_split
from sklearn.datasets import load_iris

X, y = load_iris(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)

pipe = Pipeline([
    ("scaler", StandardScaler()),       # 第1步：标准化
    ("knn", KNeighborsClassifier(n_neighbors=5)),  # 第2步：模型
])

pipe.fit(X_tr, y_tr)
print("准确率:", pipe.score(X_te, y_te))
```

### 为什么必须用 Pipeline

**不用 Pipeline 的隐患**：如果用全数据 fit scaler 再划分训练测试集，**测试集信息泄露到标准化参数里**，评估会偏乐观。

```python
# 错误
scaler.fit(X)                    # 用了全部数据
X_tr, X_te = train_test_split(X) # 再划分
# scaler 已经"见过"测试集 → 泄露

# 正确：先划分，再 fit
X_tr, X_te = train_test_split(X)
scaler.fit(X_tr)                 # 只用训练集
X_tr_s = scaler.transform(X_tr)
X_te_s = scaler.transform(X_te)
```

Pipeline 自动保证这点，**强烈推荐**。

---

## 八、ColumnTransformer：不同列不同处理

数值列标准化、分类列独热，同时进行：

```python
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.impute import SimpleImputer

df = pd.DataFrame({
    "age": [18, 20, np.nan, 25],
    "income": [5000, 8000, 6000, 9000],
    "city": ["北京","上海","广州","北京"],
})

num_cols = ["age", "income"]
cat_cols = ["city"]

preprocessor = ColumnTransformer([
    ("num", Pipeline([
        ("imp", SimpleImputer(strategy="median")),
        ("sc", StandardScaler()),
    ]), num_cols),
    ("cat", OneHotEncoder(), cat_cols),
])

X_processed = preprocessor.fit_transform(df)
print(X_processed)
```

---

## 九、保存与加载模型

```python
import joblib
joblib.dump(pipe, "model.pkl")        # 保存
model = joblib.load("model.pkl")      # 加载
```

> `joblib` 比 pickle 更适合 sklearn 模型（含 numpy 数组）。

---

## 每日练习

1. 造一个含缺失值、含类别列的小数据集，用 `SimpleImputer + OneHotEncoder` 处理。
2. 用 `StandardScaler` 标准化鸢尾花数据，对比标准化前后 KNN 准确率。
3. 用 `Pipeline` 串联 `StandardScaler + KNeighborsClassifier`，跑鸢尾花。
4. 用 `ColumnTransformer` 对一个含数值列+分类列的数据做预处理。

---

<details>
<summary>参考答案</summary>

**练习 1**

```python
import numpy as np
from sklearn.impute import SimpleImputer
from sklearn.preprocessing import OneHotEncoder

X = np.array([["北京", 1, np.nan],
              ["上海", 2, 3],
              ["广州", np.nan, 5]], dtype=object)
X_num = SimpleImputer(strategy="mean").fit_transform(X[:, 1:].astype(float))
X_cat = OneHotEncoder(sparse_output=False).fit_transform(X[:, 0:1])
print(np.hstack([X_cat, X_num]))
```

**练习 2**

```python
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
from sklearn.neighbors import KNeighborsClassifier
from sklearn.preprocessing import StandardScaler

X, y = load_iris(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)
print("不缩放:", KNeighborsClassifier().fit(X_tr, y_tr).score(X_te, y_te))
scaler = StandardScaler().fit(X_tr)
print("缩放后:", KNeighborsClassifier().fit(scaler.transform(X_tr), y_tr).score(scaler.transform(X_te), y_te))
```

**练习 3**

```python
from sklearn.pipeline import Pipeline
pipe = Pipeline([("sc", StandardScaler()), ("knn", KNeighborsClassifier())])
pipe.fit(X_tr, y_tr)
print(pipe.score(X_te, y_te))
```

**练习 4**

```python
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import StandardScaler, OneHotEncoder

df = pd.DataFrame({"age":[18,20,25], "income":[5,8,9], "city":["北京","上海","北京"]})
ct = ColumnTransformer([
    ("num", StandardScaler(), ["age","income"]),
    ("cat", OneHotEncoder(), ["city"]),
])
print(ct.fit_transform(df))
```

</details>

---

## 今日小结

- ✅ 缺失值：SimpleImputer（mean/median/most_frequent/constant）
- ✅ 编码：LabelEncoder（标签）/ OneHotEncoder（特征）/ OrdinalEncoder（有序）
- ✅ 缩放：StandardScaler / MinMaxScaler / RobustScaler（树模型不用）
- ✅ 特征选择：VarianceThreshold / SelectKBest / 模型重要性
- ✅ Pipeline 串联流程，防止数据泄露
- ✅ ColumnTransformer 不同列不同处理
- ✅ joblib 保存加载模型

明天学：监督学习-分类，三个经典算法。
