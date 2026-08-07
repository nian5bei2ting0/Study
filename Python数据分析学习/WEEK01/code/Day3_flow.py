# Day3 示例：流程控制
# 运行：python code/Day3_flow.py

# if 分支
score = int(input("分数："))
if score >= 90:
    print("优秀")
elif score >= 60:
    print("及格")
else:
    print("不及格")

# for + range
total = 0
for i in range(1, 101):
    total += i
print(f"1到100的和：{total}")   # 5050

# while
n = 5
while n > 0:
    print(n)
    n -= 1
print("发射！")

# break / continue
for i in range(10):
    if i == 5:
        break
    if i % 2 == 0:
        continue
    print(i)   # 1 3

# 九九乘法表
for i in range(1, 10):
    for j in range(1, i + 1):
        print(f"{j}x{i}={i*j}", end="\t")
    print()

# 猜数字游戏
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
