# Day 6 · 文件操作与异常处理

> 今日目标：让程序能读写文件，并能优雅地处理错误。
> 预计时间：2 小时

---

## 一、读写文本文件

### 1. 写文件

```python
# 方式一：传统写法（要手动关闭）
f = open("test.txt", "w", encoding="utf-8")
f.write("第一行\n")
f.write("第二行\n")
f.close()

# 方式二：with 写法（推荐，自动关闭）
with open("test.txt", "w", encoding="utf-8") as f:
    f.write("第一行\n")
    f.write("第二行\n")
```

> ⚠️ 永远用 `with` 写法，文件用完自动关闭，不会忘。
> ⚠️ 处理中文一定加 `encoding="utf-8"`，否则 Windows 默认 GBK 容易乱码。

### 2. 读文件

```python
# 一次性读完
with open("test.txt", "r", encoding="utf-8") as f:
    content = f.read()
print(content)

# 逐行读（推荐，大文件不占内存）
with open("test.txt", "r", encoding="utf-8") as f:
    for line in f:
        print(line.strip())   # strip 去掉换行符

# 读成列表，每行一个元素
with open("test.txt", "r", encoding="utf-8") as f:
    lines = f.readlines()
print(lines)   # ['第一行\n', '第二行\n']
```

### 3. 追加写入

```python
with open("test.txt", "a", encoding="utf-8") as f:   # a = append
    f.write("第三行\n")
```

### 模式速查

| 模式 | 含义 | 文件不存在时 |
|------|------|------------|
| `"r"` | 只读 | 报错 |
| `"w"` | 只写（覆盖） | 创建 |
| `"a"` | 追加 | 创建 |
| `"r+"` | 读写 | 报错 |

---

## 二、异常处理 try-except

### 1. 为什么需要

没有异常处理：

```python
n = int(input("数字："))   # 输入 abc 就崩溃
print(10 / n)              # 输入 0 就崩溃
```

程序一遇错就退出，用户体验差。

### 2. 基本结构

```python
try:
    n = int(input("数字："))
    print(10 / n)
except ValueError:
    print("你输入的不是数字！")
except ZeroDivisionError:
    print("不能除以0！")
```

### 3. 捕获多种异常

```python
try:
    n = int(input("数字："))
    print(10 / n)
except (ValueError, ZeroDivisionError) as e:
    print(f"出错了：{e}")
```

### 4. 完整结构

```python
try:
    # 可能出错的代码
    result = 10 / int(input("数字："))
except ValueError:
    print("输入非法")
except ZeroDivisionError:
    print("除0")
else:
    print(f"结果是 {result}")   # 没出错才执行
finally:
    print("无论如何都执行")      # 总会执行（如关闭资源）
```

### 5. 主动抛出异常

```python
def set_age(age):
    if age < 0:
        raise ValueError("年龄不能为负数")
    return age

set_age(-1)   # 抛出 ValueError
```

---

## 三、常见异常类型

| 异常 | 触发场景 |
|------|---------|
| `ValueError` | 类型对但值非法，如 `int("abc")` |
| `TypeError` | 类型不对，如 `"a" + 1` |
| `ZeroDivisionError` | 除以 0 |
| `IndexError` | 列表越界 `lst[100]` |
| `KeyError` | 字典键不存在 `d["no"]` |
| `FileNotFoundError` | 文件不存在 |
| `AttributeError` | 调用不存在的方法 |

---

## 四、实战：安全地读数字文件

```python
def read_numbers(path):
    """读取每行一个数字的文件，返回列表。"""
    numbers = []
    try:
        with open(path, "r", encoding="utf-8") as f:
            for i, line in enumerate(f, 1):
                line = line.strip()
                if not line:
                    continue
                try:
                    numbers.append(float(line))
                except ValueError:
                    print(f"第{i}行不是数字：{line}，已跳过")
    except FileNotFoundError:
        print(f"文件不存在：{path}")
    return numbers

print(read_numbers("nums.txt"))
```

> 注意：内层 try 处理单行错误（跳过继续），外层 try 处理文件不存在（直接返回空）。

---

## 五、with 的本质（了解）

`with` 后面的对象必须有 `__enter__` 和 `__exit__` 方法，进入时调 enter，离开时调 exit（即使出错也会调）。除了文件，还常用于数据库连接、锁等需要"用完释放"的资源。

---

## 每日练习

1. 写程序：让用户输入 5 行文字，存到 `note.txt`，再读出来打印。
2. 写程序：读一个数字文件（每行一个数），计算总和，处理"文件不存在"和"某行不是数字"两种错误。
3. 写函数 `safe_divide(a, b)`，b 为 0 时返回 None 而不是报错。
4. 写程序：让用户输入年龄，负数时用 raise 抛出 ValueError，捕获后提示重新输入。

---

<details>
<summary>参考答案</summary>

**练习 1**

```python
with open("note.txt", "w", encoding="utf-8") as f:
    for i in range(5):
        f.write(input(f"第{i+1}行：") + "\n")

with open("note.txt", "r", encoding="utf-8") as f:
    print(f.read())
```

**练习 2**

```python
total = 0
try:
    with open("nums.txt", "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                total += float(line)
            except ValueError:
                print(f"跳过非数字：{line}")
except FileNotFoundError:
    print("文件不存在")
print(f"总和：{total}")
```

**练习 3**

```python
def safe_divide(a, b):
    try:
        return a / b
    except ZeroDivisionError:
        return None

print(safe_divide(10, 0))   # None
```

**练习 4**

```python
while True:
    try:
        age = int(input("年龄："))
        if age < 0:
            raise ValueError("年龄不能为负")
        print(f"你的年龄是 {age}")
        break
    except ValueError as e:
        print(f"输入有误：{e}，请重试")
```

</details>

---

## 今日小结

- ✅ `with open(...) as f` 读写文件，自动关闭
- ✅ 模式 r / w / a，处理中文加 `encoding="utf-8"`
- ✅ `try / except / else / finally` 异常处理
- ✅ `raise` 主动抛异常
- ✅ 常见异常类型

明天：综合实战，把前 6 天学的东西做成一个完整小程序。
