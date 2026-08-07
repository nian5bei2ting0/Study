# Day 3 · Pandas 入门：Series 与 DataFrame

> 今日目标：理解 Pandas 两大核心数据结构，学会创建和索引。
> 预计时间：2 小时

---

## 一、为什么用 Pandas

NumPy 处理纯数值很棒，但真实数据有列名、行索引、混合类型、缺失值。Pandas 在 NumPy 之上加了"表格语义"，让数据分析像写 SQL 一样自然。

```python
import pandas as pd
import numpy as np
```

约定俗成：`import pandas as pd`。

---

## 二、Series：一维带标签数组

### 1. 创建

```python
s = pd.Series([10, 20, 30, 40])
print(s)
# 0    10
# 1    20
# 2    30
# 3    40
# dtype: int64
```

左边是**索引**（默认 0,1,2...），右边是**值**。

### 2. 自定义索引

```python
s = pd.Series([10, 20, 30], index=["a", "b", "c"])
print(s["b"])   # 20
print(s[["a", "c"]])   # 取多个
```

### 3. 从字典创建

```python
s = pd.Series({"小明": 90, "小红": 85, "小刚": 78})
print(s["小红"])   # 85
```

### 4. 常用属性与方法

```python
s = pd.Series([10, 20, 30, 40])
print(s.index)    # 索引
print(s.values)   # 值（NumPy 数组）
print(s.dtype)    # 类型
print(s.shape)    # (4,)

print(s.sum(), s.mean(), s.max(), s.min())
print(s.describe())   # 统计摘要：count mean std min 25% 50% 75% max
```

### 5. 运算与筛选

```python
s = pd.Series([1, 2, 3, 4, 5])
print(s + 10)         # 每个加10
print(s * 2)
print(s[s > 3])       # 布尔筛选
print(s.isin([2, 4])) # 是否在列表中
```

---

## 三、DataFrame：二维带标签表格

最常用的结构，相当于 Excel 表。

### 1. 创建

```python
df = pd.DataFrame({
    "name": ["小明", "小红", "小刚"],
    "age": [18, 19, 20],
    "score": [90, 85, 78],
})
print(df)
```

```
   name  age  score
0   小明   18     90
1   小红   19     85
2   小刚   20     78
```

### 2. 从二维列表创建

```python
data = [
    ["小明", 18, 90],
    ["小红", 19, 85],
    ["小刚", 20, 78],
]
df = pd.DataFrame(data, columns=["name", "age", "score"])
```

### 3. 查看数据

```python
print(df.head())      # 前5行（默认5）
print(df.head(2))     # 前2行
print(df.tail(2))     # 后2行
print(df.shape)       # (3, 3)
print(df.columns)     # 列名
print(df.index)       # 行索引
print(df.dtypes)      # 每列类型
print(df.info())      # 整体信息
print(df.describe())  # 数值列统计摘要
```

> `df.info()` 和 `df.describe()` 是拿到数据后**最先敲的两个命令**。

---

## 四、选取数据

### 1. 选列

```python
df["name"]            # 返回 Series
df[["name", "age"]]   # 返回 DataFrame（双括号！）
```

> ⚠️ `df["name"]` 是 Series，`df[["name"]]` 是 DataFrame，差一对括号。

### 2. 选行

```python
df.iloc[0]        # 第0行（按位置）
df.iloc[0:2]      # 第0~1行
df.loc[0]         # 索引为0的行（按标签）
```

### 3. 行列同时选（重点）

```python
df.iloc[0:2, 1:3]        # 前2行、第1~2列（按位置）
df.loc[0:1, ["name", "score"]]  # 索引0~1行、指定列名
```

口诀：
- **`iloc` 用数字位置**（i = integer）
- **`loc` 用标签名**

### 4. 条件筛选

```python
df[df["score"] > 80]                          # 分数>80
df[(df["score"] > 80) & (df["age"] < 19)]     # 多条件
df[df["name"].isin(["小明", "小红"])]          # 在列表中
df[df["name"].str.startswith("小")]            # 字符串方法
```

---

## 五、增加与删除

```python
df = pd.DataFrame({"name": ["小明", "小红"], "age": [18, 19]})

# 加列
df["city"] = ["北京", "上海"]
df["is_adult"] = df["age"] >= 18

# 加行（不推荐循环用，慢；后面会讲 concat）
df.loc[2] = ["小刚", 20, "广州", True]

# 删列
df.drop("city", axis=1, inplace=True)   # 删列
df.drop(0, axis=0)                       # 删第0行

# 用 del
del df["is_adult"]
```

> ⚠️ `drop` 默认返回新对象，不修改原数据。`inplace=True` 才原地修改。
> 优先用 `df = df.drop(...)` 而非 `inplace=True`，更清晰。

---

## 六、索引操作

```python
df = pd.DataFrame({"name": ["小明","小红"], "age": [18,19]}, index=["a","b"])
print(df.loc["a"])

# 重置索引
df_reset = df.reset_index(drop=True)   # 0,1,2...

# 设某列为索引
df_idx = df.set_index("name")          # 用 name 当行索引
```

---

## 每日练习

1. 创建一个 DataFrame：3 个学生，含姓名、语文、数学、英语三科成绩，输出每行。
2. 选出"语文 > 80"的学生。
3. 加一列"总分"=三科之和，加一列"平均分"。
4. 用 `iloc` 取第 1 行第 2 列，用 `loc` 取索引为 0 的行的"数学"列。

---

<details>
<summary>参考答案</summary>

**练习 1**

```python
import pandas as pd
df = pd.DataFrame({
    "name": ["小明", "小红", "小刚"],
    "语文": [85, 92, 78],
    "数学": [90, 88, 85],
    "英语": [82, 95, 80],
})
print(df)
```

**练习 2**

```python
print(df[df["语文"] > 80])
```

**练习 3**

```python
df["总分"] = df["语文"] + df["数学"] + df["英语"]
df["平均分"] = df[["语文", "数学", "英语"]].mean(axis=1)
print(df)
```

**练习 4**

```python
print(df.iloc[1, 2])          # 第1行第2列
print(df.loc[0, "数学"])      # 索引0行的数学列
```

</details>

---

## 今日小结

- ✅ Series：一维带标签数组，index + values
- ✅ DataFrame：二维表格，columns + index
- ✅ `df.info() / df.describe()` 是看数据第一步
- ✅ 选列 `df["col"]`，选行 `df.iloc[] / df.loc[]`
- ✅ iloc 按位置，loc 按标签
- ✅ 条件筛选 `df[df["col"] > x]`，多条件用 `& |` 加括号
- ✅ 加列直接赋值，删用 `drop`

明天学：从文件读数据 + 数据清洗（缺失值/重复/类型）。
