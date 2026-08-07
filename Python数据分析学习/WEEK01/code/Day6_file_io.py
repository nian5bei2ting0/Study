# Day6 示例：文件操作与异常处理
# 运行：python code/Day6_file_io.py

# 写文件
with open("note.txt", "w", encoding="utf-8") as f:
    for i in range(5):
        f.write(input(f"第{i+1}行：") + "\n")

# 读文件
with open("note.txt", "r", encoding="utf-8") as f:
    print("--- 文件内容 ---")
    for line in f:
        print(line.strip())


# 安全除法
def safe_divide(a, b):
    try:
        return a / b
    except ZeroDivisionError:
        return None


print(safe_divide(10, 0))   # None
print(safe_divide(10, 2))   # 5.0


# 安全读取数字文件
def read_numbers(path):
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


# 主动抛异常 + 捕获重试
while True:
    try:
        age = int(input("年龄："))
        if age < 0:
            raise ValueError("年龄不能为负")
        print(f"你的年龄是 {age}")
        break
    except ValueError as e:
        print(f"输入有误：{e}，请重试")
