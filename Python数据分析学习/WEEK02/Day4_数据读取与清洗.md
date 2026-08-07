# Day 4 · 数据读取与清洗

> 今日目标：从 csv/excel 读数据，处理缺失值、重复值、类型转换。
> 预计时间：2 小时

---

## 一、读取数据

### 1. 读 CSV

```python
import pandas as pd

df = pd.read_csv("data.csv")
df = pd.read_csv("data.csv", encoding="utf-8")     # 中文
df = pd.read_csv("data.csv", encoding="gbk")      # Windows 中文常用
df = pd.read_csv("data.csv", sep=",")              # 分隔符
df = pd.read_csv("data.csv", nrows=100)            # 只读前100行（大文件探查）
df = pd.read_csv("data.csv", usecols=["name","age"])  # 只读指定列
```

### 2. 读 Excel

```python
df = pd.read_excel("data.xlsx", sheet_name="Sheet1")
# 需要 openpyxl：pip install openpyxl
```

### 3. 写出数据

```python
df.to_csv("out.csv", index=False, encoding="utf-8-sig")  # 不写索引，中文不乱码
df.to_excel("out.xlsx", index=False)
```

> ⚠️ `to_csv` 默认会写一列索引，**通常加 `index=False`**。
> ⚠️ 中文 csv 用 `utf-8-sig`，Excel 打开才不乱码。

---

## 二、拿到数据第一步：先看

```python
df = pd.read_csv("data.csv")

print(df.shape)        # 多少行多少列
print(df.head())       # 前5行
print(df.info())       # 列名、类型、非空数、内存
print(df.describe())   # 数值列统计
print(df.columns)      # 列名
```

> 这 5 个命令是数据分析的"开场白"，永远先敲。

---

## 三、处理缺失值

### 1. 查看缺失

```python
df.isnull()           # 整表是否缺失
df.isnull().sum()     # 每列缺失数
df.isnull().sum() / len(df)   # 每列缺失率
df[df["age"].isnull()]       # age 缺失的行
```

### 2. 删除缺失

```python
df.dropna()                  # 删任意有缺失的行
df.dropna(subset=["age"])    # 只看 age 列
df.dropna(thresh=3)           # 至少3个非空才保留
df.dropna(how="all")          # 全部缺失才删
```

### 3. 填充缺失

```python
df["age"].fillna(0)                       # 填0
df["age"].fillna(df["age"].mean())        # 填均值
df["age"].fillna(df["age"].median())      # 填中位数
df["city"].fillna("未知")                  # 类别填"未知"
df.fillna(method="ffill")                 # 用前一个值填（时间序列常用）
df.fillna(method="bfill")                 # 用后一个值填
```

> ⚠️ `fillna` 返回新对象，要保存就 `df = df.fillna(...)` 或 `inplace=True`。

---

## 四、处理重复值

```python
df.duplicated()            # 是否重复行
df.duplicated().sum()      # 重复行数
df.drop_duplicates()       # 删重复行
df.drop_duplicates(subset=["name"])   # 按 name 去重
df.drop_duplicates(subset=["name"], keep="last")  # 保留最后一条
```

---

## 五、类型转换

### 1. 查看与转换

```python
df.dtypes                       # 每列类型
df["age"] = df["age"].astype(int)         # 转 int
df["price"] = df["price"].astype(float)   # 转 float
df["is_vip"] = df["is_vip"].astype(bool)
```

### 2. 字符串转日期

```python
df["date"] = pd.to_datetime(df["date"])
df["date"] = pd.to_datetime(df["date"], format="%Y-%m-%d")
```

转成日期后才能用 `.dt` 访问器：

```python
df["year"] = df["date"].dt.year
df["month"] = df["date"].dt.month
df["weekday"] = df["date"].dt.day_name()
```

### 3. 字符串列清理

```python
df["name"] = df["name"].str.strip()           # 去空白
df["price"] = df["price"].str.replace("￥", "").astype(float)  # "￥99" -> 99.0
df["city"] = df["city"].str.replace("市", "")  # "北京市" -> "北京"
```

### 4. category 类型省内存

```python
df["city"] = df["city"].astype("category")   # 类别少时大幅省内存
```

---

## 六、列名整理

```python
df.columns = df.columns.str.strip().str.lower()   # 统一小写去空白

# 改个别列名
df.rename(columns={"Name": "name", "Age": "age"}, inplace=True)
```

---

## 七、实战：清洗一份脏数据

```python
import pandas as pd

# 假设 data.csv 有缺失、重复、字符串数字
df = pd.read_csv("data.csv")
print("原始：", df.shape)

# 1. 去重
df = df.drop_duplicates()

# 2. 列名规范化
df.columns = df.columns.str.strip().str.lower()

# 3. 缺失值处理
df["age"] = df["age"].fillna(df["age"].median())
df["city"] = df["city"].fillna("未知")

# 4. 类型转换
df["age"] = df["age"].astype(int)
df["price"] = df["price"].astype(float)

# 5. 日期
df["date"] = pd.to_datetime(df["date"], errors="coerce")  # 解析失败变 NaT
df = df.dropna(subset=["date"])

print("清洗后：", df.shape)
df.to_csv("clean.csv", index=False, encoding="utf-8-sig")
```

> `errors="coerce"` 让无法解析的值变成缺失而不是报错，清洗时很有用。

---

## 每日练习

1. 用代码生成一份"脏数据" csv（含缺失、重复、字符串数字"￥99"），保存后读取并清洗。
2. 统计清洗前后行数差异，输出每列缺失率。
3. 把日期字符串列转成 datetime，并新增 year、month 两列。
4. 把"城市"列转成 category 类型，比较转换前后内存占用（`df.memory_usage(deep=True)`）。

---

<details>
<summary>参考答案</summary>

**练习 1+2**

```python
import pandas as pd

# 造脏数据
df = pd.DataFrame({
    "name": ["小明", "小红", "小明", "小刚", None],
    "age": [18, None, 18, 20, 19],
    "price": ["￥99", "￥199", "￥99", "￥299", "￥99"],
    "city": ["北京", "上海", "北京", "广州", "北京"],
    "date": ["2025-01-01", "2025-02-01", "2025-01-01", "2025-03-01", "2025-01-01"],
})
df.to_csv("dirty.csv", index=False, encoding="utf-8-sig")

# 读取清洗
df = pd.read_csv("dirty.csv", encoding="utf-8-sig")
print("原始行数：", len(df))
print("缺失率：\n", df.isnull().mean())

df = df.drop_duplicates()
df["name"] = df["name"].fillna("匿名")
df["age"] = df["age"].fillna(df["age"].median()).astype(int)
df["price"] = df["price"].str.replace("￥", "").astype(float)
print("清洗后行数：", len(df))
```

**练习 3**

```python
df["date"] = pd.to_datetime(df["date"])
df["year"] = df["date"].dt.year
df["month"] = df["date"].dt.month
```

**练习 4**

```python
print(df.memory_usage(deep=True))
df["city"] = df["city"].astype("category")
print(df.memory_usage(deep=True))   # city 列内存明显变小
```

</details>

---

## 今日小结

- ✅ `read_csv / read_excel / to_csv / to_excel`
- ✅ 看数据五件套：shape / head / info / describe / columns
- ✅ 缺失值：isnull / dropna / fillna
- ✅ 重复值：duplicated / drop_duplicates
- ✅ 类型转换：astype / to_datetime / str.replace
- ✅ `.dt` 访问器取年月日
- ✅ category 类型省内存

明天学：筛选、排序、分组聚合——数据分析最常用的操作。
