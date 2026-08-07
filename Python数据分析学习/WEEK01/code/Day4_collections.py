# Day4 示例：数据结构
# 运行：python code/Day4_collections.py

# 列表
fruits = ["apple", "banana", "cherry"]
fruits.append("date")
fruits.insert(1, "mango")
print(fruits)
print(fruits[1:3])

nums = [3, 1, 4, 1, 5, 9, 2, 6]
print(sum(nums), max(nums), min(nums))
print(sorted(nums))

# 元组
point = (3, 4)
x, y = point
print(x, y)

# 字典
person = {"name": "小明", "age": 18, "city": "北京"}
print(person["name"])
person["email"] = "a@b.com"
for key, value in person.items():
    print(f"{key}: {value}")

# 集合去重
nums = [1, 2, 2, 3, 3, 3]
print(list(set(nums)))

# 嵌套：列表里放字典
students = [
    {"name": "小明", "score": 90},
    {"name": "小红", "score": 85},
    {"name": "小刚", "score": 78},
]
best = max(students, key=lambda s: s["score"])
print(f"第一名：{best['name']}")

# 单词计数练习
text = input("英文：").lower()
words = text.split()
count = {}
for w in words:
    count[w] = count.get(w, 0) + 1
print(count)
