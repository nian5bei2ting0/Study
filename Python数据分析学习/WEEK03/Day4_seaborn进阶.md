# Day 4 · seaborn 进阶

> 今日目标：掌握关系图、回归图、热力图、分面对图——EDA 的核心武器。
> 预计时间：2 小时

---

## 一、关系图 relplot

看两个连续变量的关系，支持散点和折线两种：

```python
import seaborn as sns
import matplotlib.pyplot as plt
plt.rcParams["font.sans-serif"] = ["SimHei"]
plt.rcParams["axes.unicode_minus"] = False

sns.set_theme(style="whitegrid")
tips = sns.load_dataset("tips")

# 散点
sns.relplot(data=tips, x="total_bill", y="tip", hue="time", style="sex", size="size")
plt.show()

# 折线（自动聚合均值+置信区间）
sns.relplot(data=tips, x="size", y="total_bill", kind="line")
plt.show()
```

| 参数 | 含义 |
|------|------|
| `hue` | 颜色分组 |
| `style` | 标记形状分组 |
| `size` | 点大小映射数值 |
| `col` | 按列拆子图 |
| `kind` | "scatter"(默认) / "line" |

---

## 二、scatterplot 进阶

```python
sns.scatterplot(data=tips, x="total_bill", y="tip",
                hue="time", style="sex", size="size",
                sizes=(20, 200), alpha=0.7)
plt.show()
```

`sizes=(20, 200)` 控制点大小范围，`alpha` 处理重叠。

---

## 三、回归图 regplot / lmplot

自动拟合回归线 + 置信区间：

```python
sns.regplot(data=tips, x="total_bill", y="tip", scatter_kws={"alpha":0.5})
plt.show()
```

```python
# 按分组拟合多条线
sns.lmplot(data=tips, x="total_bill", y="tip", hue="smoker")
plt.show()
```

### 多项式回归

```python
sns.regplot(data=tips, x="total_bill", y="tip", order=2)   # 二次拟合
plt.show()
```

### 残差图 residplot

```python
sns.residplot(data=tips, x="total_bill", y="tip")
plt.axhline(0, color="red", linestyle="--")
plt.show()
```

---

## 四、pairplot 散点矩阵（EDA 神器）

一次性看所有数值列两两关系 + 对角线分布：

```python
sns.pairplot(tips, hue="time", diag_kind="kde")
plt.show()
```

> 拿到新数据**第一个该敲的 seaborn 命令**，一眼看出哪些变量相关、有无分类可分。

---

## 五、jointplot 联合分布

两个变量的联合分布 + 各自边缘分布：

```python
sns.jointplot(data=tips, x="total_bill", y="tip", kind="scatter")
plt.show()

sns.jointplot(data=tips, x="total_bill", y="tip", kind="kde")   # 等高线
plt.show()

sns.jointplot(data=tips, x="total_bill", y="tip", kind="reg")    # 加回归
plt.show()

sns.jointplot(data=tips, x="total_bill", y="tip", kind="hex")   # 六边形分箱
plt.show()
```

---

## 六、热力图 heatmap

矩阵数据可视化，**相关性分析标配**：

```python
# 相关性矩阵
corr = tips.select_dtypes(include="number").corr()
print(corr)

sns.heatmap(corr, annot=True, cmap="coolwarm", center=0, fmt=".2f")
plt.title("相关性矩阵")
plt.show()
```

| 参数 | 含义 |
|------|------|
| `annot` | 显示数值 |
| `cmap` | 颜色映射 |
| `center` | 中心值（相关性用 0） |
| `fmt` | 数值格式 |
| `vmin/vmax` | 颜色范围 |

### 数据透视热力图

```python
pivot = tips.pivot_table(values="total_bill", index="day", columns="time", aggfunc="mean")
sns.heatmap(pivot, annot=True, cmap="YlGnBu", fmt=".1f")
plt.title("各天各时段平均账单")
plt.show()
```

---

## 七、聚类热力图 clustermap

自动行列聚类，发现数据模式：

```python
sns.clustermap(corr, annot=True, cmap="coolwarm")
plt.show()
```

---

## 八、分面对图 FacetGrid + map_dataframe

更灵活的分面，支持多参数：

```python
g = sns.FacetGrid(tips, col="day", row="time", hue="sex", height=3)
g.map_dataframe(sns.scatterplot, x="total_bill", y="tip")
g.add_legend()
plt.show()
```

---

## 九、PairGrid 自定义矩阵

```python
g = sns.PairGrid(tips, hue="time")
g.map_upper(sns.scatterplot)        # 上三角散点
g.map_lower(sns.kdeplot)            # 下三角密度
g.map_diag(sns.histplot)            # 对角线直方
g.add_legend()
plt.show()
```

---

## 十、调色板

```python
# 查看调色板
sns.color_palette("Set2")

# 数值型用渐变
sns.scatterplot(data=tips, x="total_bill", y="tip",
                hue="size", palette="viridis")

# 分类型用定性
sns.scatterplot(data=tips, x="total_bill", y="tip",
                hue="day", palette="Set2")
```

调色板选择：
- **数值连续**：viridis / plasma / coolwarm
- **数值发散**（有正负）：RdBu / coolwarm（配 center=0）
- **分类**：Set1 / Set2 / pastel / deep

---

## 每日练习

用 `tips` 数据集完成：

1. 用 `relplot` 画 `total_bill` vs `tip`，`hue` 用 `time`，`col` 用 `sex`。
2. 用 `lmplot` 按 `smoker` 分组拟合 `total_bill` vs `tip` 的回归线。
3. 用 `pairplot` 看所有数值列关系，`hue` 用 `time`。
4. 计算数值列相关性矩阵，用 `heatmap` 画出来，加 `annot`。
5. 用 `jointplot` 画 `total_bill` vs `tip` 的 KDE 联合分布。

---

<details>
<summary>参考答案</summary>

```python
import seaborn as sns
import matplotlib.pyplot as plt
plt.rcParams["font.sans-serif"] = ["SimHei"]
plt.rcParams["axes.unicode_minus"] = False
sns.set_theme(style="whitegrid")

tips = sns.load_dataset("tips")

# 1
sns.relplot(data=tips, x="total_bill", y="tip", hue="time", col="sex")
plt.show()

# 2
sns.lmplot(data=tips, x="total_bill", y="tip", hue="smoker")
plt.show()

# 3
sns.pairplot(tips, hue="time", diag_kind="kde")
plt.show()

# 4
corr = tips.select_dtypes(include="number").corr()
sns.heatmap(corr, annot=True, cmap="coolwarm", center=0, fmt=".2f")
plt.title("相关性矩阵"); plt.show()

# 5
sns.jointplot(data=tips, x="total_bill", y="tip", kind="kde")
plt.show()
```

</details>

---

## 今日小结

- ✅ `relplot` 关系图（scatter/line）
- ✅ `regplot / lmplot` 回归图
- ✅ `pairplot` 散点矩阵（EDA 神器）
- ✅ `jointplot` 联合分布
- ✅ `heatmap` 相关性/透视热力图
- ✅ `clustermap` 聚类热力
- ✅ `FacetGrid / PairGrid` 灵活分面
- ✅ 调色板：连续/发散/分类

明天进入 EDA 方法论：拿到新数据该怎么系统化探索。
