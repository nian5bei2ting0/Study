# Day 4 · 数据结构：列表 / 元组 / 字典 / 集合

> 今日目标：学会用 Python 存"一组数据"。这是后续写程序的基础。
> 预计时间：2 小时

---

## 一、列表 list（最常用）

有序、可修改、可重复。

### 1. 创建与访问

```python
fruits = ["apple", "banana", "cherry"]
print(fruits[0])        # apple  从0开始
print(fruits[-1])       # cherry 最后一个
print(len(fruits))      # 3
```

### 2. 修改与添加

```python
fruits = ["apple", "banana"]
fruits[0] = "Apple"        # 修改
fruits.append("cherry")    # 末尾添加
fruits.insert(1, "mango")  # 在位置1插入
print(fruits)             # ['Apple', 'mango', 'banana', 'cherry']
```

### 3. 删除

```python
fruits = ["apple", "banana", "cherry"]
fruits.remove("banana")   # 按值删
del fruits[0]             # 按位置删
fruits.pop()              # 删除并返回最后一个
```

### 4. 切片（和字符串一样）

```python
nums = [0, 1, 2, 3, 4, 5]
print(nums[1:4])    # [1, 2, 3]
print(nums[:3])     # [0, 1, 2]
print(nums[::-1])   # [5, 4, 3, 2, 1, 0]  反转
```

### 5. 常用操作

```python
nums = [3, 1, 4, 1, 5, 9, 2, 6]
print(sum(nums))     # 31
print(max(nums))     # 9
print(min(nums))     # 1
print(sorted(nums))  # [1, 1, 2, 3, 4, 5, 6, 9]  返回新列表
nums.sort()          # 原地排序
nums.reverse()       # 反转
print(5 in nums)     # True  是否存在
```

### 6. 遍历

```python
for f in fruits:
    print(f)

for i, f in enumerate(fruits):   # 同时拿索引和值
    print(i, f)
```

---

## 二、元组 tuple

有序、**不可修改**、可重复。一旦创建不能改。

```python
point = (3, 4)
print(point[0])     # 3
x, y = point        # 解包
print(x, y)          # 3 4

# point[0] = 5       # 报错！元组不能改
```

用途：固定不变的数据（坐标、配置）、函数返回多个值。

> 经验：能用元组就用元组，比列表更安全、更快。

---

## 三、字典 dict（超级常用）

键值对，用 key 查 value，**查找极快**。

### 1. 创建与访问

```python
person = {"name": "小明", "age": 18, "city": "北京"}
print(person["name"])       # 小明
print(person.get("phone"))   # None  不存在的键用 get 不报错
print(person.get("phone", "未填写"))  # 未填写  设默认值
```

### 2. 增删改

```python
person = {"name": "小明", "age": 18}
person["age"] = 19          # 改
person["email"] = "a@b.com" # 增
del person["email"]         # 删
```

### 3. 遍历

```python
person = {"name": "小明", "age": 18, "city": "北京"}

for key in person:                  # 只遍历键
    print(key)

for key, value in person.items():   # 键和值都遍历
    print(f"{key}: {value}")
```

### 4. 常用方法

```python
print(person.keys())     # 所有键
print(person.values())   # 所有值
print("name" in person)  # True  判断键是否存在
```

> ⚠️ 判断的是**键**不是值：`"小明" in person` 是 False。

---

## 四、集合 set

无序、**不重复**。常用于去重和判断成员关系。

```python
nums = [1, 2, 2, 3, 3, 3]
unique = set(nums)
print(unique)        # {1, 2, 3}
print(list(unique))  # [1, 2, 3]  转回列表

s1 = {1, 2, 3}
s2 = {3, 4, 5}
print(s1 & s2)   # {3}  交集
print(s1 | s2)   # {1,2,3,4,5}  并集
print(s1 - s2)  # {1, 2}  差集
```

---

## 五、四种结构对比

| 结构 | 有序 | 可修改 | 可重复 | 典型用途 |
|------|:---:|:---:|:---:|---------|
| list 列表 | ✅ | ✅ | ✅ | 一组同类数据 |
| tuple 元组 | ✅ | ❌ | ✅ | 固定不变的数据 |
| dict 字典 | ❌ | ✅ | 键不重复 | 用 key 查 value |
| set 集合 | ❌ | ✅ | ❌ | 去重 / 成员判断 |

---

## 六、嵌套结构（真实场景常见）

```python
students = [
    {"name": "小明", "score": 90},
    {"name": "小红", "score": 85},
    {"name": "小刚", "score": 78},
]

for s in students:
    print(f"{s['name']}：{s['score']}分")

# 找最高分
best = max(students, key=lambda s: s["score"])
print(f"第一名：{best['name']}")
```

`lambda` 是临时小函数，明天会讲，这里先认识一下。

---

## 每日练习

1. 输入 5 个数字存进列表，输出最大值、最小值、平均值。
2. 给定列表 `[1,2,3,4,5,6,7,8,9,10]`，分别输出其中的奇数列表和偶数列表。
3. 用字典存 3 个同学的姓名和成绩，输出平均分和最高分同学姓名。
4. 输入一段英文，统计每个单词出现次数（用字典）。提示：`.split()` 切词。

---

<details>
<summary>参考答案</summary>

**练习 1**

```python
nums = []
for i in range(5):
    nums.append(int(input(f"第{i+1}个数：")))
print(f"最大{max(nums)} 最小{min(nums)} 平均{sum(nums)/len(nums)}")
```

**练习 2**

```python
nums = list(range(1, 11))
odd = [n for n in nums if n % 2 == 1]   # 列表推导式，明天讲
even = [n for n in nums if n % 2 == 0]
print(odd, even)
```

**练习 3**

```python
scores = {"小明": 90, "小红": 85, "小刚": 78}
avg = sum(scores.values()) / len(scores)
top = max(scores, key=scores.get)
print(f"平均分{avg}，第一名{top}")
```

**练习 4**

```python
text = input("英文：").lower()
words = text.split()
count = {}
for w in words:
    count[w] = count.get(w, 0) + 1
print(count)
```

</details>

---

## 今日小结

- ✅ list：有序可改，最常用，append/insert/remove/sort
- ✅ tuple：有序不可改，用于固定数据
- ✅ dict：键值对，用 key 查 value 极快，items() 遍历
- ✅ set：去重和成员判断，支持交并差
- ✅ 嵌套结构：列表里放字典，真实场景标配

明天学：函数，把重复代码封装起来。
