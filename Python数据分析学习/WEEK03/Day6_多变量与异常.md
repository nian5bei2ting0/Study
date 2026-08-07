# Day 6 · 多变量探索与异常检测可视化

> 今日目标：用可视化探索多变量关系，识别异常点。
> 预计时间：2 小时

---

## 一、相关性分析

### 1. 计算相关系数

```python
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
plt.rcParams["font.sans-serif"] = ["SimHei"]
plt.rcParams["axes.unicode_minus"] = False
sns.set_theme(style="whitegrid")

tips = sns.load_dataset("tips")
num = tips.select_dtypes(include="number")
print(num.corr())   # 默认 Pearson
```

### 2. 三种相关系数

```python
print(num.corr(method="pearson"))    # 线性相关（默认）
print(num.corr(method="spearman"))   # 秩相关（单调，抗异常）
print(num.corr(method="kendall"))    # 肯德尔秩
```

| 方法 | 适用 |
|------|------|
| pearson | 线性关系，连续变量 |
| spearman | 单调关系（含非线性），有异常时更稳 |
| kendall | 小样本，秩数据 |

### 3. 相关性热力图

```python
corr = num.corr()
sns.heatmap(corr, annot=True, cmap="coolwarm", center=0, fmt=".2f",
            square=True, vmin=-1, vmax=1)
plt.title("相关性矩阵")
plt.show()
```

读法：
- 接近 1 = 强正相关
- 接近 -1 = 强负相关
- 接近 0 = 无线性相关

> ⚠️ **相关 ≠ 因果**，只能说明一起变化。

---

## 二、散点矩阵 pairplot

```python
sns.pairplot(tips, hue="time", vars=["total_bill","tip","size"], diag_kind="kde")
plt.show()
```

`vars` 指定要画的列，避免列太多图太密。

> 列数 > 8 时 pairplot 会很慢很挤，先用相关性热力图筛掉无关列。

---

## 三、分组对比可视化

### 1. 分类 vs 数值

```python
sns.boxplot(data=tips, x="day", y="total_bill", hue="time")
plt.show()

sns.violinplot(data=tips, x="day", y="total_bill", hue="sex", split=True)
plt.show()
```

### 2. 数值 vs 数值 + 分类着色

```python
sns.scatterplot(data=tips, x="total_bill", y="tip", hue="time", style="sex", size="size")
plt.show()
```

### 3. 多维分面

```python
g = sns.FacetGrid(tips, col="day", row="time", hue="sex", height=3)
g.map_dataframe(sns.scatterplot, x="total_bill", y="tip")
g.add_legend()
plt.show()
```

---

## 四、异常检测可视化

### 1. 箱线图识别异常

```python
sns.boxplot(x=tips["total_bill"])
plt.title("箱线图识别异常")
plt.show()

# 提取异常值
q1, q3 = tips["total_bill"].quantile([0.25, 0.75])
iqr = q3 - q1
outliers = tips[(tips["total_bill"] < q1 - 1.5*iqr) | (tips["total_bill"] > q3 + 1.5*iqr)]
print(f"IQR 法识别异常 {len(outliers)} 条")
```

### 2. 3σ 原则

```python
mean, std = tips["total_bill"].mean(), tips["total_bill"].std()
outliers_3sigma = tips[(tips["total_bill"] - mean).abs() > 3*std]
print(f"3σ 法识别异常 {len(outliers_3sigma)} 条")
```

### 3. Z-score 可视化

```python
tips["z"] = (tips["total_bill"] - mean) / std
sns.histplot(tips["z"], bins=30)
plt.axvline(3, color="red", linestyle="--")
plt.axvline(-3, color="red", linestyle="--")
plt.title("Z-score 分布（红线外为异常）")
plt.show()
```

### 4. 散点图圈出异常

```python
plt.scatter(tips.index, tips["total_bill"], c="blue", alpha=0.5)
plt.scatter(outliers.index, outliers["total_bill"], c="red", s=50, label="异常")
plt.legend()
plt.title("异常点高亮")
plt.show()
```

---

## 五、二维密度与等高线

```python
sns.kdeplot(data=tips, x="total_bill", y="tip", fill=True, cmap="Blues")
plt.show()

sns.jointplot(data=tips, x="total_bill", y="tip", kind="kde")
plt.show()
```

> 二维密度能看出数据集中的"热点区域"，比散点图更清晰。

---

## 六、时间序列异常

```python
import numpy as np
idx = pd.date_range("2025-01-01", periods=60, freq="D")
sales = pd.Series(np.sin(np.arange(60)/5)*50 + 100 + np.random.normal(0,5,60), index=idx)
sales.iloc[20] = 200   # 造一个异常点

plt.figure(figsize=(12,4))
plt.plot(sales)
plt.scatter([sales.index[20]], [sales.iloc[20]], color="red", s=80, label="异常")
plt.legend()
plt.title("时间序列异常点")
plt.show()
```

---

## 七、变量重要性初探

虽然没建模，但可以用相关性大小粗筛重要变量：

```python
target = "tip"
corr_with_target = num.corr()[target].drop(target).sort_values(ascending=False)
print(corr_with_target)
corr_with_target.plot.bar()
plt.title(f"各变量与 {target} 的相关性")
plt.show()
```

---

## 每日练习

用 `tips` 数据集完成：

1. 计算数值列 Pearson 相关性，画热力图。
2. 用 `pairplot` 看 `total_bill / tip / size` 三列关系，`hue` 用 `time`。
3. 用 IQR 法识别 `total_bill` 的异常值，在散点图上用红色标出。
4. 用 3σ 法识别 `tip` 的异常值，对比两种方法结果。
5. 算各数值列与 `tip` 的相关性，画条形图排序。

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

tips = sns.load_dataset("tips")
num = tips.select_dtypes(include="number")

# 1
sns.heatmap(num.corr(), annot=True, cmap="coolwarm", center=0, fmt=".2f")
plt.title("相关性"); plt.show()

# 2
sns.pairplot(tips, hue="time", vars=["total_bill","tip","size"], diag_kind="kde")
plt.show()

# 3
q1, q3 = tips["total_bill"].quantile([0.25, 0.75])
iqr = q3 - q1
outliers = tips[(tips["total_bill"] < q1-1.5*iqr) | (tips["total_bill"] > q3+1.5*iqr)]
plt.scatter(tips.index, tips["total_bill"], alpha=0.5, label="正常")
plt.scatter(outliers.index, outliers["total_bill"], color="red", s=60, label="异常")
plt.legend(); plt.title("IQR 异常"); plt.show()

# 4
mean, std = tips["tip"].mean(), tips["tip"].std()
out_3s = tips[(tips["tip"]-mean).abs() > 3*std]
print(f"IQR:{len(outliers)} 3σ:{len(out_3s)}")

# 5
corr_tip = num.corr()["tip"].drop("tip").sort_values(ascending=False)
corr_tip.plot.bar(); plt.title("与 tip 相关性"); plt.show()
```

</details>

---

## 今日小结

- ✅ 三种相关系数：pearson / spearman / kendall
- ✅ 相关性热力图（center=0, vmin=-1, vmax=1）
- ✅ pairplot 散点矩阵
- ✅ 分组对比：box/violin/scatter + hue + FacetGrid
- ✅ 异常检测：IQR / 3σ / Z-score
- ✅ 二维密度 KDE
- ✅ 变量重要性初筛

明天：综合实战，把全周知识做成一份完整 EDA 报告。
