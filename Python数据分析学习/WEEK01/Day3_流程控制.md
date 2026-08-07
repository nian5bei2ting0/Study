# Day 3 · 流程控制

> 今日目标：让程序能"做选择"和"重复执行"。这是编程的核心。
> 预计时间：2 小时

---

## 一、if 条件语句

### 1. 基本结构

```python
score = 75

if score >= 90:
    print("优秀")
elif score >= 60:
    print("及格")
else:
    print("不及格")
```

### 2. 缩进非常重要！

Python 用**缩进（4 个空格）**表示代码块，不用大括号 `{}`。下面两种写法效果完全不同：

```python
# 写法 A
if score >= 60:
    print("及格")
    print("继续努力")   # 这行属于 if 内部

# 写法 B
if score >= 60:
    print("及格")
print("继续努力")       # 这行不属于 if，永远执行
```

> ⚠️ 新手 90% 的报错是缩进错误。统一用 **4 个空格**，不要混 Tab 和空格。

### 3. 嵌套 if

```python
age = 25
has_ticket = True

if age >= 18:
    if has_ticket:
        print("可以入场")
    else:
        print("请先买票")
else:
    print("未成年人不可入")
```

---

## 二、for 循环

### 1. 遍历

```python
for ch in "Python":
    print(ch)
# P y t h o n  逐字符打印
```

### 2. range() 生成数字序列

```python
for i in range(5):       # 0 1 2 3 4   0~4，不含5
    print(i)

for i in range(1, 6):    # 1 2 3 4 5   1~5
    print(i)

for i in range(0, 10, 2):# 0 2 4 6 8   步长2
    print(i)
```

口诀：`range(start, stop, step)`，**同样左闭右开**。

### 3. 累加示例

```python
total = 0
for i in range(1, 101):   # 1+2+...+100
    total += i
print(total)   # 5050
```

---

## 三、while 循环

```python
n = 5
while n > 0:
    print(n)
    n -= 1
print("发射！")
# 5 4 3 2 1 发射！
```

`while` 后面跟条件，条件为 True 就一直循环。**一定要有让条件变 False 的语句**（这里 `n -= 1`），否则死循环。

---

## 四、break 和 continue

```python
# break：直接跳出整个循环
for i in range(10):
    if i == 5:
        break
    print(i)   # 0 1 2 3 4

# continue：跳过本次，继续下一次
for i in range(5):
    if i == 2:
        continue
    print(i)   # 0 1 3 4
```

> 新手建议：能用 `if + break/continue` 写清楚的，比硬写复杂条件更易读。

---

## 五、else 与循环的搭配（进阶，了解即可）

```python
for i in range(10):
    if i == 100:
        break
else:
    print("循环正常结束，没遇到 break")
```

循环的 `else` 在**没被 break 打断**时执行。不常用，知道有这东西即可。

---

## 六、嵌套循环：九九乘法表

```python
for i in range(1, 10):
    for j in range(1, i + 1):
        print(f"{j}x{i}={i*j}", end="\t")
    print()   # 换行
```

输出：
```
1x1=1	
1x2=2	2x2=4	
1x3=3	2x3=6	3x3=9	
...
```

`print()` 默认结尾换行，用 `end="\t"` 改成制表符，让一行打印多个。

---

## 每日练习

1. 输入一个数，判断它是奇数还是偶数。
2. 输出 1~100 中所有 7 的倍数。
3. 求 1~100 中所有偶数的和。
4. 输入一个正整数 n，输出它的阶乘 n!（如 5! = 1×2×3×4×5 = 120）。
5. 猜数字游戏：程序里定一个秘密数字（比如 42），让用户反复猜，提示"大了"或"小了"，猜中为止。

---

<details>
<summary>参考答案</summary>

**练习 1**

```python
n = int(input("一个数："))
if n % 2 == 0:
    print("偶数")
else:
    print("奇数")
```

**练习 2**

```python
for i in range(1, 101):
    if i % 7 == 0:
        print(i)
```

**练习 3**

```python
total = 0
for i in range(1, 101):
    if i % 2 == 0:
        total += i
print(total)   # 2550
```

**练习 4**

```python
n = int(input("n："))
result = 1
for i in range(1, n + 1):
    result *= i
print(f"{n}! = {result}")
```

**练习 5**

```python
secret = 42
while True:
    guess = int(input("猜："))
    if guess == secret:
        print("猜中了！")
        break
    elif guess > secret:
        print("大了")
    else:
        print("小了")
```

</details>

---

## 今日小结

- ✅ if / elif / else 条件分支
- ✅ 缩进决定代码块（4 空格）
- ✅ for + range() 循环（左闭右开）
- ✅ while 循环（注意别死循环）
- ✅ break 跳出、continue 跳过

明天学：列表、元组、字典、集合——Python 处理"一组数据"的利器。
