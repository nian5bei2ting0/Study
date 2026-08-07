# Day 6 · 合并、透视、时间序列

> 今日目标：学会把多张表合并、做透视表、处理时间序列。
> 预计时间：2 小时

---

## 一、合并表 merge

类似 SQL 的 JOIN，按某列连接两张表。

```python
import pandas as pd

employees = pd.DataFrame({
    "emp_id": [1, 2, 3, 4],
    "name": ["小明","小红","小刚","小李"],
    "dept_id": [10, 20, 10, 30],
})

departments = pd.DataFrame({
    "dept_id": [10, 20, 40],
    "dept_name": ["销售","技术","人事"],
})

# 内连接：两表都有的 dept_id
pd.merge(employees, departments, on="dept_id", how="inner")

# 左连接：保留左表全部，右表无则填 NaN
pd.merge(employees, departments, on="dept_id", how="left")

# 外连接：全保留
pd.merge(employees, departments, on="dept_id", how="outer")

# 列名不同时
pd.merge(employees, departments, left_on="dept_id", right_on="dept_id")
```

| how | 含义 |
|-----|------|
| inner | 交集（默认） |
| left | 保留左表 |
| right | 保留右表 |
| outer | 并集，缺失填 NaN |

---

## 二、拼接 concat

不按列连接，直接上下/左右堆。

```python
a = pd.DataFrame({"x": [1,2], "y": [3,4]})
b = pd.DataFrame({"x": [5,6], "y": [7,8]})

pd.concat([a, b])             # 上下堆（默认 axis=0）
pd.concat([a, b], ignore_index=True)  # 重置索引
pd.concat([a, b], axis=1)     # 左右堆
```

> merge 是"按 key 连接"，concat 是"机械堆叠"，注意区别。

---

## 三、透视表 pivot_table

类似 Excel 透视表，把长表转宽表。

```python
df = pd.DataFrame({
    "date": ["2025-01-01","2025-01-01","2025-01-02","2025-01-02"]*2,
    "city": ["北京","上海"]*4,
    "product": ["A","A","A","A","B","B","B","B"],
    "sales": [100, 200, 150, 180, 80, 90, 70, 110],
})

# 行=city，列=product，值=sales之和
p = df.pivot_table(values="sales", index="city", columns="product", aggfunc="sum")
print(p)
```

```
product   A    B
city
上海    380   200
北京    250   150
```

### 多个聚合

```python
df.pivot_table(values="sales", index="city", columns="product",
                aggfunc=["sum", "mean"])
```

### 多层行索引

```python
df.pivot_table(values="sales", index=["city","product"], aggfunc="sum")
```

### 透视表的反操作 melt

```python
wide = df.pivot_table(values="sales", index="city", columns="product", aggfunc="sum").reset_index()
long = wide.melt(id_vars="city", var_name="product", value_name="sales")
```

> `melt` 把宽表转长表，是数据整理常用操作。

---

## 四、交叉表 crosstab

统计两个分类变量的频数：

```python
pd.crosstab(df["city"], df["product"])
pd.crosstab(df["city"], df["product"], margins=True)   # 加合计
pd.crosstab(df["city"], df["product"], normalize="index")  # 按行归一化
```

---

## 五、时间序列

### 1. 生成时间索引

```python
dates = pd.date_range("2025-01-01", periods=10, freq="D")   # 10天
s = pd.Series(range(10), index=dates)
```

### 2. 重采样 resample

```python
s.resample("D").sum()    # 按天
s.resample("W").sum()    # 按周
s.resample("M").mean()   # 按月
```

freq 常用：`D`天 `W`周 `M`月末 `MS`月初 `H`小时 `min`分 `Q`季末 `Y`年末。

### 3. 滚动窗口 rolling

```python
s.rolling(window=3).mean()    # 3天移动平均
s.rolling(window=3).sum()
```

### 4. 时间差 shift

```python
s.shift(1)     # 整体下移1位（前一行）
s.shift(-1)    # 上移1位（后一行）
s - s.shift(1) # 环比变化
```

### 5. 实战：销售月度趋势

```python
df = pd.DataFrame({
    "date": pd.date_range("2025-01-01", periods=180, freq="D"),
    "sales": (pd.Series(range(180)) * 10 + 1000 + pd.Series(range(180)).apply(lambda x: x%7*50)),
})

df = df.set_index("date")

monthly = df.resample("M").sum()
print(monthly)

df["7日均线"] = df["sales"].rolling(7).mean()
print(df.head(10))
```

---

## 六、apply 与 map

### 1. Series.map 逐元素映射

```python
s = pd.Series([1,2,3,4])
print(s.map({1:"一", 2:"二"}))   # 字典映射
print(s.map(lambda x: x**2))    # 函数映射
```

### 2. DataFrame.apply 按行/列应用

```python
df = pd.DataFrame({"a":[1,2,3], "b":[4,5,6]})
df.apply(sum)              # 默认按列求和
df.apply(sum, axis=1)      # 按行求和
df.apply(lambda col: col.max()-col.min())   # 每列极差
```

### 3. applymap 逐元素（已被 map 替代）

```python
df = df.map(lambda x: f"{x:.2f}")   # 新版用 map
```

---

## 每日练习

1. 创建员工表和部门表，用 merge 做左连接，找出没有部门的员工。
2. 用上面的销售数据做透视表：行=城市，列=产品，值=销售额均值。
3. 生成 30 天的随机销售数据，按周重采样求和，并算 7 日移动平均。
4. 用 melt 把透视表结果转回长表。

---

<details>
<summary>参考答案</summary>

**练习 1**

```python
emp = pd.DataFrame({"emp_id":[1,2,3], "name":["A","B","C"], "dept_id":[10,20,None]})
dept = pd.DataFrame({"dept_id":[10,20], "dept_name":["销售","技术"]})
merged = pd.merge(emp, dept, on="dept_id", how="left")
print(merged[merged["dept_name"].isna()])
```

**练习 2**

```python
df = pd.DataFrame({
    "city":["北京","上海"]*4,
    "product":["A","A","B","B"]*2,
    "sales":[100,200,80,90,150,180,70,110],
})
print(df.pivot_table(values="sales", index="city", columns="product", aggfunc="mean"))
```

**练习 3**

```python
import numpy as np
idx = pd.date_range("2025-01-01", periods=30, freq="D")
s = pd.Series(np.random.randint(50, 200, 30), index=idx)
print(s.resample("W").sum())
print(s.rolling(7).mean())
```

**练习 4**

```python
wide = df.pivot_table(values="sales", index="city", columns="product", aggfunc="mean").reset_index()
long = wide.melt(id_vars="city", var_name="product", value_name="sales")
print(long)
```

</details>

---

## 今日小结

- ✅ `merge` 按 key 连接（inner/left/right/outer）
- ✅ `concat` 机械堆叠（axis=0 上下，axis=1 左右）
- ✅ `pivot_table` 长转宽，`melt` 宽转长
- ✅ `crosstab` 频数表
- ✅ 时间序列：date_range / resample / rolling / shift
- ✅ `map` 逐元素，`apply` 按行/列

明天：综合实战，把全周知识做成一个电商销售分析项目。
