# Day 5 · EDA 方法论与数据画像

> 今日目标：建立系统化的 EDA 流程，拿到任何新数据都知道从哪开始。
> 预计时间：2 小时

---

## 一、什么是 EDA

**EDA（Exploratory Data Analysis，探索性数据分析）**：在建模之前，用统计 + 可视化手段**理解数据**的过程。

目标不是得出结论，而是回答：
- 数据长什么样？规模、字段、类型？
- 数据质量如何？缺失、重复、异常？
- 各变量分布如何？偏态、长尾、多峰？
- 变量之间有什么关系？
- 有没有明显的模式或异常？

> EDA 做得越扎实，后续建模越少踩坑。**跳过 EDA 直接建模 = 灾难**。

---

## 二、EDA 标准流程（6 步）

```
1. 数据概览    →  2. 数据质量   →  3. 单变量分析
                                          ↓
6. 多变量分析  ←  5. 衍生指标   ←  4. 分布与异常
```

### Step 1：数据概览

```python
import pandas as pd
df = pd.read_csv("data.csv")

print(df.shape)        # 多少行多少列
print(df.head())       # 前5行
print(df.info())       # 类型、非空数、内存
print(df.describe())   # 数值列统计
print(df.describe(include="object"))   # 分类列统计
```

### Step 2：数据质量

```python
# 缺失
missing = df.isnull().sum()
missing_pct = df.isnull().mean() * 100
quality = pd.DataFrame({"缺失数": missing, "缺失率%": missing_pct})
print(quality[quality["缺失数"] > 0])

# 重复
print("重复行数:", df.duplicated().sum())

# 唯一值（基数）
print(df.nunique())
```

### Step 3：单变量分析

数值列：
```python
df["amount"].describe()
df["amount"].hist(bins=30)
df["amount"].plot.box()
```

分类列：
```python
df["city"].value_counts()
df["city"].value_counts().plot.bar()
```

### Step 4：分布与异常

```python
import seaborn as sns
sns.histplot(df["amount"], kde=True)
sns.boxplot(x=df["amount"])

# 偏度峰度
print(df["amount"].skew(), df["amount"].kurt())
```

### Step 5：衍生指标

```python
df["log_amount"] = np.log1p(df["amount"])   # 对数变换处理长尾
df["amount_bucket"] = pd.cut(df["amount"], bins=[0,100,1000,10000,np.inf],
                              labels=["小","中","大","超大"])
```

### Step 6：多变量分析（明天详讲）

```python
sns.pairplot(df.select_dtypes("number"))
sns.heatmap(df.select_dtypes("number").corr(), annot=True)
```

---

## 三、数据画像模板

把 Step 1-2 的结果汇总成一份"数据画像"，是 EDA 的第一份交付物：

```python
def data_profile(df):
    """生成数据画像字典。"""
    profile = {
        "行数": len(df),
        "列数": df.shape[1],
        "重复行": int(df.duplicated().sum()),
        "缺失列数": int((df.isnull().sum() > 0).sum()),
        "数值列": list(df.select_dtypes(include="number").columns),
        "分类列": list(df.select_dtypes(include="object").columns),
        "时间列": list(df.select_dtypes(include="datetime").columns),
    }

    # 每列画像
    col_profile = []
    for c in df.columns:
        col_profile.append({
            "列名": c,
            "类型": str(df[c].dtype),
            "缺失率": f"{df[c].isnull().mean():.2%}",
            "唯一值数": df[c].nunique(),
            "示例": str(df[c].dropna().iloc[0]) if len(df) else "",
        })
    return profile, pd.DataFrame(col_profile)

profile, col_df = data_profile(df)
print(profile)
print(col_df)
```

---

## 四、缺失值可视化

```python
import seaborn as sns
import matplotlib.pyplot as plt

# 每列缺失数条形图
missing = df.isnull().sum()
missing[missing > 0].sort_values().plot.barh()
plt.title("各列缺失数")
plt.show()

# 缺失矩阵（看缺失模式）
sns.heatmap(df.isnull(), cbar=False, yticklabels=False, cmap="viridis")
plt.title("缺失值位置")
plt.show()
```

> 缺失矩阵能看出缺失是随机还是集中在某些行/列，**对决定填充策略很重要**。

---

## 五、单变量分布诊断

### 1. 看偏态

```python
skew = df["amount"].skew()
if skew > 1:
    print("右偏明显，建议对数变换")
elif skew < -1:
    print("左偏明显")
else:
    print("近似对称")
```

### 2. 对数变换处理长尾

```python
df["log_amount"] = np.log1p(df["amount"])   # log(1+x) 避免 0 报错
fig, axes = plt.subplots(1, 2, figsize=(12, 4))
sns.histplot(df["amount"], ax=axes[0]); axes[0].set_title("原始")
sns.histplot(df["log_amount"], ax=axes[1]); axes[1].set_title("对数变换后")
plt.show()
```

### 3. 分位数-分位数图 QQ plot

判断是否近似正态：

```python
from scipy import stats
import matplotlib.pyplot as plt
stats.probplot(df["amount"].dropna(), plot=plt)
plt.show()
```

点落在对角线上 = 正态分布，偏离 = 偏态/长尾。

---

## 六、分类变量探索

```python
# 频数
print(df["city"].value_counts())

# Top-N 占比
top_share = df["city"].value_counts(normalize=True).head(5).sum()
print(f"Top5 城市占比 {top_share:.1%}")

# 可视化
df["city"].value_counts().head(10).plot.bar()
plt.title("城市分布 Top10")
plt.show()
```

---

## 七、EDA 报告骨架

每份 EDA 报告建议包含：

1. **数据概览**：行数列数、时间范围、来源
2. **数据质量**：缺失/重复/异常汇总
3. **单变量分析**：每个关键变量的分布图
4. **多变量分析**：相关性、分组对比
5. **关键发现**：3-5 条业务洞察
6. **后续建议**：是否可建模、需补什么数据

---

## 每日练习

用 WEEK02 Day7 生成的 `sales_raw.csv`（或自己造数据）完成：

1. 写一个 `data_profile` 函数，输出数据画像。
2. 画缺失值矩阵热力图，判断缺失是否随机。
3. 对 `amount` 列做单变量分析：describe + 直方图 + 箱线图 + 偏度。
4. 若 `amount` 右偏，做对数变换，对比变换前后分布。
5. 写一份 200 字的 EDA 报告骨架。

---

<details>
<summary>参考答案</summary>

```python
import pandas as pd
import numpy as np
import seaborn as sns
import matplotlib.pyplot as plt
plt.rcParams["font.sans-serif"] = ["SimHei"]
plt.rcParams["axes.unicode_minus"] = False
sns.set_theme(style="whitegrid")

df = pd.read_csv("sales_raw.csv", encoding="utf-8-sig")

# 1
def data_profile(df):
    profile = {"行数": len(df), "列数": df.shape[1],
               "重复行": int(df.duplicated().sum())}
    col_df = pd.DataFrame([{
        "列名": c, "类型": str(df[c].dtype),
        "缺失率": f"{df[c].isnull().mean():.2%}",
        "唯一值数": df[c].nunique(),
    } for c in df.columns])
    return profile, col_df

print(data_profile(df))

# 2
sns.heatmap(df.isnull(), cbar=False, yticklabels=False, cmap="viridis")
plt.title("缺失值位置"); plt.show()

# 3
df["amount"] = pd.to_numeric(df["unit_price"], errors="coerce") * df["quantity"]
print(df["amount"].describe())
fig, axes = plt.subplots(1, 2, figsize=(12,4))
sns.histplot(df["amount"].dropna(), bins=30, ax=axes[0])
sns.boxplot(x=df["amount"].dropna(), ax=axes[1])
plt.show()
print("偏度:", df["amount"].skew())

# 4
df["log_amount"] = np.log1p(df["amount"])
fig, axes = plt.subplots(1, 2, figsize=(12,4))
sns.histplot(df["amount"].dropna(), bins=30, ax=axes[0]); axes[0].set_title("原始")
sns.histplot(df["log_amount"].dropna(), bins=30, ax=axes[1]); axes[1].set_title("对数")
plt.show()
```

</details>

---

## 今日小结

- ✅ EDA 6 步流程：概览→质量→单变量→分布→衍生→多变量
- ✅ 数据画像模板（行/列/类型/缺失/基数）
- ✅ 缺失值可视化（条形图 + 矩阵热力图）
- ✅ 偏态诊断 + 对数变换
- ✅ QQ plot 判断正态
- ✅ EDA 报告骨架

明天学：多变量关系与异常检测可视化。
