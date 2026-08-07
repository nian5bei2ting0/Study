# Day 2 · 运算符与字符串

> 今日目标：掌握各类运算符，学会用字符串方法处理文本。
> 预计时间：1.5 小时

---

## 一、算术运算符

```python
print(7 + 2)    # 9   加
print(7 - 2)    # 5   减
print(7 * 2)    # 14  乘
print(7 / 2)    # 3.5 除（结果总是 float）
print(7 // 2)   # 3   整除（向下取整）
print(7 % 2)   # 1   取余（模）
print(7 ** 2)  # 49  幂（7的2次方）
```

### 重点
- `/` 永远返回浮点数，即使能整除：`6 / 2` 得 `3.0` 不是 `3`。
- `%` 取余非常常用：判断奇偶、每隔 N 个取一次等。
- `**` 是幂运算，不是 `^`（`^` 在 Python 里是位运算，别用错）。

---

## 二、比较运算符

结果都是**布尔值** `True` / `False`：

```python
print(3 > 2)       # True
print(3 < 2)       # False
print(3 == 3)      # True   等于（注意是两个等号）
print(3 != 3)      # False  不等于
print(3 >= 3)      # True
print(2 <= 1)      # False
```

> ⚠️ 新手最常犯的错：把 `==` 写成 `=`。`=` 是赋值，`==` 才是判断相等。

---

## 三、逻辑运算符

```python
age = 20
print(age > 18 and age < 60)   # True   且
print(age < 18 or age > 60)    # False  或
print(not True)                # False  非
```

Python 用英文单词 `and / or / not`，不是 `&& / || / !`。

---

## 四、赋值运算符

```python
x = 10
x += 5    # 等价于 x = x + 5，现在 x = 15
x -= 3    # x = 12
x *= 2    # x = 24
x //= 5   # x = 4
x %= 3    # x = 1
```

`+=` 最常用，比如累加计数。

---

## 五、字符串：文本的处理

字符串是 Python 里最重要的类型之一，方法非常多，今天学最常用的。

### 1. 拼接与重复

```python
a = "Hello"
b = "World"
print(a + " " + b)    # Hello World
print(a * 3)          # HelloHelloHello  重复3次
```

### 2. 索引与切片（重点）

字符串可以按位置取字符，**从 0 开始**：

```python
s = "Python"
#    0123456
#    654321(负数从右数)
print(s[0])     # P   第1个
print(s[-1])    # n   最后一个
print(s[1:4])   # yth 第2到第4个（左闭右开，不含第5个）
print(s[:3])    # Pyt 省略开头=从头开始
print(s[3:])    # hon 省略结尾=到末尾
print(s[:])     # Python  整个
```

切片口诀：**左闭右开，省头从头，省尾到尾**。

### 3. 常用方法

```python
s = "  Hello, World  "
print(s.strip())           # "Hello, World"  去两端空白
print(s.upper())           # "  HELLO, WORLD  "  全大写
print(s.lower())           # 全小写
print(s.replace("World", "Python"))  # 替换
print(s.split(","))        # 按逗号切分成列表
print(len(s))              # 字符串长度
print("World" in s)        # True  判断是否包含
print(s.startswith("  H")) # True
```

### 4. 判断字符串内容

```python
"123".isdigit()     # True  是否全是数字
"abc".isalpha()     # True  是否全是字母
"abc123".isalnum()  # True  是否字母或数字
```

---

## 六、字符串与数字互转

```python
n = 100
s = str(n)        # 数字 -> 字符串 "100"
n2 = int("200")   # 字符串 -> 数字 200
```

> ⚠️ `int("12.5")` 会报错，要先用 `float("12.5")` 再 `int()`。

---

## 每日练习

1. 输入一个手机号字符串，判断它是否是 11 位且全是数字，输出"合法"或"不合法"。
2. 输入一句话，统计其中字母 `a`（不区分大小写）出现了几次。
3. 给定 `s = "Python-3.12"`，用切片取出 `"3.12"`。
4. 把 `"hello world"` 转成 `"Hello World"`（每个单词首字母大写）。提示：查 `.title()` 方法。

---

<details>
<summary>参考答案</summary>

**练习 1**

```python
phone = input("手机号：")
if len(phone) == 11 and phone.isdigit():
    print("合法")
else:
    print("不合法")
```

**练习 2**

```python
s = input("一句话：")
print(s.lower().count("a"))
```

**练习 3**

```python
s = "Python-3.12"
print(s[7:])   # 3.12
```

**练习 4**

```python
print("hello world".title())   # Hello World
```

</details>

---

## 今日小结

- ✅ 算术 / 比较 / 逻辑 / 赋值运算符
- ✅ 字符串索引与切片（左闭右开）
- ✅ 字符串常用方法：strip / upper / lower / replace / split / count / in
- ✅ 数字与字符串互转

明天学：流程控制（if / for / while），让程序能"做判断"和"重复执行"。
