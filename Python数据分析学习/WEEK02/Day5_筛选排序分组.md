# Day 5 · 筛选、排序、分组聚合

> 今日目标：掌握数据分析最常用的三大操作——筛选、排序、分组聚合。
> 预计时间：2 小时

---

## 一、数据准备

```python
import pandas as pd

df = pd.DataFrame({
    "name": ["小明","小红","小刚","小李","小王","小张","小陈","小刘"],
    "dept": ["销售","销售","技术","技术","销售","技术","人事","人事"],
    "city": ["北京","上海","北京","广州","上海","广州","北京","上海"],
    "salary": [8000, 9000, 15000, 13000, 8500, 16000, 7000, 7500],
    "years": [1, 2, 5, 3, 2, 6, 1, 2],
})
```

---

## 二、筛选数据

### 1. 单条件

```python
df[df["salary"] > 10000]
```

### 2. 多条件（& | ~，加括号）

```python
df[(df["salary"] > 10000) & (df["city"] == "北京")]
df[(df["dept"] == "销售") | (df["dept"] == "技术")]
df[~(df["city"] == "北京")]   # 不在北京
```

### 3. isin 多值匹配

```python
df[df["city"].isin(["北京", "上海"])]
```

### 4. query 字符串写法（更直观）

```python
df.query("salary > 10000 and city == '北京'")
df.query("dept in ['销售','技术']")
df.query("salary > @threshold")   # 用 @ 引用外部变量
```

### 5. 字符串条件

```python
df[df["name"].str.startswith("小")]
df[df["name"].str.contains("红")]
df[df["city"].str.len() == 2]
```

### 6. loc 条件 + 选列

```python
df.loc[df["salary"] > 10000, ["name", "salary"]]   # 筛行+选列
```

---

## 三、排序

### 1. 按一列

```python
df.sort_values("salary")                       # 升序
df.sort_values("salary", ascending=False)     # 降序
```

### 2. 按多列

```python
df.sort_values(["dept", "salary"], ascending=[True, False])
# 部门升序，部门内薪资降序
```

### 3. 按索引排序

```python
df.sort_index()
```

### 4. 按值排名

```python
df["salary_rank"] = df["salary"].rank(ascending=False, method="dense")
# method: average(默认) min max first dense
```

---

## 四、分组聚合 groupby（重点！）

数据分析最强大的工具，相当于 SQL 的 GROUP BY。

### 1. 基本分组聚合

```python
df.groupby("dept")["salary"].mean()      # 每部门平均薪资
df.groupby("dept")["salary"].sum()       # 每部门薪资总和
df.groupby("dept")["salary"].agg(["mean","max","min","count"])
```

### 2. 多列分组

```python
df.groupby(["dept", "city"])["salary"].mean()
# 部门+城市 双层分组
```

### 3. 多列多聚合 agg

```python
df.groupby("dept").agg({
    "salary": ["mean", "max"],
    "years": ["mean", "sum"],
})
```

### 4. 自定义聚合函数

```python
df.groupby("dept")["salary"].agg(lambda s: s.max() - s.min())  # 极差
df.groupby("dept")["salary"].agg(["mean", lambda s: s.max()-s.min()])
```

### 5. agg 命名

```python
df.groupby("dept").agg(
    平均薪资=("salary", "mean"),
    最高薪资=("salary", "max"),
    平均工龄=("years", "mean"),
)
```

> 这种写法输出列名清晰，**强烈推荐**。

### 6. transform 不聚合返回原长度

```python
df["dept_avg"] = df.groupby("dept")["salary"].transform("mean")
# 每个人都附上自己部门的平均薪资，行数不变
```

### 7. apply 任意操作

```python
df.groupby("dept").apply(lambda g: g.nlargest(2, "salary"))
# 每部门薪资最高的2人
```

---

## 五、常见聚合函数

| 函数 | 含义 |
|------|------|
| `count` | 非空数 |
| `sum` | 求和 |
| `mean` / `median` | 均值 / 中位数 |
| `min` / `max` | 最小 / 最大 |
| `std` / `var` | 标准差 / 方差 |
| `nunique` | 唯一值个数 |
| `first` / `last` | 第一个 / 最后一个 |
| `size` | 组大小（含空） |

---

## 六、reset_index 让分组结果变回 DataFrame

```python
result = df.groupby("dept")["salary"].mean()
print(type(result))   # Series

result = df.groupby("dept")["salary"].mean().reset_index()
print(type(result))   # DataFrame，列名是 dept / salary
```

> 分组后通常加 `reset_index()` 让结果更规整，方便后续使用。

---

## 七、实战：销售分析

```python
# 每部门人数、平均薪资、薪资总和
summary = df.groupby("dept").agg(
    人数=("name", "count"),
    平均薪资=("salary", "mean"),
    薪资总和=("salary", "sum"),
).reset_index()
print(summary)

# 薪资 Top 3
top3 = df.nlargest(3, "salary")
print(top3)

# 每城市薪资最高的人
best_per_city = df.loc[df.groupby("city")["salary"].idxmax()]
print(best_per_city)
```

`idxmax()` 返回最大值的索引，配合 `loc` 取整行——**取每组最优的常用套路**。

---

## 每日练习

用上面的 df 数据完成：

1. 筛选"北京或上海"且"工龄 >= 2"的员工。
2. 按薪资降序排序，输出前 3 名。
3. 按部门分组，输出每部门的人数、平均薪资、最高薪资，列名清晰。
4. 给每个员工加一列"部门平均薪资"（用 transform）。
5. 找出每个城市薪资最高的员工。

---

<details>
<summary>参考答案</summary>

**练习 1**

```python
print(df[df["city"].isin(["北京","上海"]) & (df["years"] >= 2)])
```

**练习 2**

```python
print(df.sort_values("salary", ascending=False).head(3))
```

**练习 3**

```python
print(df.groupby("dept").agg(
    人数=("name","count"),
    平均薪资=("salary","mean"),
    最高薪资=("salary","max"),
).reset_index())
```

**练习 4**

```python
df["部门平均薪资"] = df.groupby("dept")["salary"].transform("mean")
print(df)
```

**练习 5**

```python
print(df.loc[df.groupby("city")["salary"].idxmax()])
```

</details>

---

## 今日小结

- ✅ 筛选：单条件、多条件 `& |`、`isin`、`query`、字符串方法
- ✅ 排序：`sort_values` 单列多列、`rank` 排名
- ✅ groupby：单列/多列分组、`agg` 多聚合、命名聚合
- ✅ `transform` 不聚合返回原长度
- ✅ `idxmax / idxmin` 取每组最优
- ✅ `reset_index` 让分组结果变 DataFrame

明天学：合并多张表、透视表、时间序列。
