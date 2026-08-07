# Day 3 · seaborn 入门

> 今日目标：理解 seaborn 与 matplotlib 的关系，掌握主题、分布图、统计图。
> 预计时间：2 小时

---

## 一、为什么用 seaborn

| | matplotlib | seaborn |
|---|-----------|---------|
| 定位 | 底层绘图库 | 统计绘图库 |
| 代码 | 繁琐 | 简洁 |
| 默认样式 | 朴素 | 美观 |
| 数据 | numpy 数组 | DataFrame（直接用列名） |
| 统计功能 | 无 | 自动聚合/拟合/置信区间 |

seaborn 是 matplotlib 的"高级封装"，**专为数据分析设计**。约定俗成：`import seaborn as sns`。

```python
import seaborn as sns
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

plt.rcParams["font.sans-serif"] = ["SimHei"]
plt.rcParams["axes.unicode_minus"] = False
```

---

## 二、主题与样式

```python
sns.set_theme(style="whitegrid", palette="pastel")
# style: darkgrid / whitegrid / dark / white / ticks
# palette: deep / muted / pastel / bright / dark / colorblind
```

> 一行 `set_theme` 就能让所有后续图变好看，**脚本开头加一次即可**。

---

## 三、内置数据集

seaborn 自带一些练习数据集，免造数据：

```python
tips = sns.load_dataset("tips")   # 餐厅小费数据
print(tips.head())
print(tips.columns)
# total_bill, tip, sex, smoker, day, time, size
```

> 第一次加载会从网络下载，需联网。无法联网可用前面学过的造数据方法。

---

## 四、分布图：直方图 + KDE

### 1. histplot 直方图

```python
sns.histplot(tips["total_bill"], bins=30, kde=True)
plt.title("账单金额分布")
plt.show()
```

`kde=True` 自动叠加核密度估计曲线（平滑的分布曲线）。

### 2. kdeplot 密度曲线

```python
sns.kdeplot(data=tips, x="total_bill", hue="time", fill=True)
plt.title("午餐/晚餐账单分布")
plt.show()
```

`hue` 按某列分组着色——**seaborn 最强大的参数**。

### 3. displot 分布图（推荐）

```python
sns.displot(data=tips, x="total_bill", hue="time", kind="kde")
# kind: hist(默认) / kde / ecdf
plt.show()
```

### 4. ecdf 累积分布

```python
sns.displot(data=tips, x="total_bill", kind="ecdf")
plt.show()
```

---

## 五、箱线图与小提琴图

### 1. boxplot

```python
sns.boxplot(data=tips, x="day", y="total_bill", hue="time")
plt.title("各天账单分布")
plt.show()
```

### 2. violinplot 小提琴图

箱线图 + 密度曲线，信息更丰富：

```python
sns.violinplot(data=tips, x="day", y="total_bill")
plt.title("各天账单分布（小提琴）")
plt.show()
```

### 3. boxenplot 大数据箱线

数据量大时比 boxplot 更准确显示尾部：

```python
sns.boxenplot(data=tips, x="day", y="total_bill")
plt.show()
```

---

## 六、计数图 countplot

直接统计分类频数，不用先 groupby：

```python
sns.countplot(data=tips, x="day", hue="time")
plt.title("各天就餐次数")
plt.show()
```

---

## 七、条形图 barplot

自动按类别计算均值并加置信区间：

```python
sns.barplot(data=tips, x="day", y="total_bill", hue="time")
plt.title("各天平均账单")
plt.show()
```

> 默认算均值，竖线是 95% 置信区间。改聚合函数用 `estimator`：
> `sns.barplot(data=tips, x="day", y="total_bill", estimator=np.median)`

---

## 八、点图 pointplot

类似 barplot 但用点和误差线，看趋势更直观：

```python
sns.pointplot(data=tips, x="day", y="total_bill", hue="time")
plt.title("各天平均账单趋势")
plt.show()
```

---

## 九、分面 FacetGrid

按某列拆成多张子图，**EDA 利器**：

```python
g = sns.FacetGrid(tips, col="time", row="sex")
g.map(sns.histplot, "total_bill")
plt.show()
```

---

## 十、选图速查

| 想看 | seaborn 函数 |
|------|------------|
| 单变量分布 | histplot / kdeplot |
| 分类分布 | boxplot / violinplot |
| 分类频数 | countplot |
| 分类均值 | barplot / pointplot |
| 多子图 | FacetGrid |

---

## 每日练习

用 `tips` 数据集完成：

1. 画 `total_bill` 的直方图 + KDE。
2. 按 `day` 分组画 `total_bill` 的箱线图，`hue` 用 `time`。
3. 画 `day` 的计数图，`hue` 用 `sex`。
4. 用 `barplot` 画各天平均小费，`estimator` 用中位数。
5. 用 `FacetGrid` 按 `time` 和 `sex` 拆成 2×2 子图画 `tip` 分布。

---

<details>
<summary>参考答案</summary>

```python
import seaborn as sns
import matplotlib.pyplot as plt
import numpy as np
plt.rcParams["font.sans-serif"] = ["SimHei"]
plt.rcParams["axes.unicode_minus"] = False

sns.set_theme(style="whitegrid")
tips = sns.load_dataset("tips")

# 1
sns.histplot(tips["total_bill"], bins=30, kde=True)
plt.title("账单分布"); plt.show()

# 2
sns.boxplot(data=tips, x="day", y="total_bill", hue="time")
plt.title("各天账单"); plt.show()

# 3
sns.countplot(data=tips, x="day", hue="sex")
plt.title("各天就餐次数"); plt.show()

# 4
sns.barplot(data=tips, x="day", y="tip", estimator=np.median)
plt.title("各天平均小费（中位数）"); plt.show()

# 5
g = sns.FacetGrid(tips, col="time", row="sex")
g.map(sns.histplot, "tip")
plt.show()
```

</details>

---

## 今日小结

- ✅ `sns.set_theme` 一行美化
- ✅ 内置数据集 `load_dataset`
- ✅ 分布图：histplot / kdeplot / displot
- ✅ 分类图：boxplot / violinplot / countplot / barplot / pointplot
- ✅ `hue` 按列分组着色（最常用参数）
- ✅ `FacetGrid` 分面多子图

明天学：seaborn 进阶——关系图、回归图、热力图、分面。
