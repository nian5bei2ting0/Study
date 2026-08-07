# Day7 综合项目：LLM 标注 + sklearn 分类
# 运行：python code/Day7_ml_project.py
# 详细讲解见 Day7_提示词工程实战.md
"""
项目流程：
1. 用 LLM 给一批评论打情感标签（正/负/中）—— 此处用模拟标签
2. 把 LLM 标签当训练数据，训 sklearn 分类器
3. 评估分类器，实现"小模型学大模型"
"""
import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report

# ===== 1. 模拟数据（实际可用 LLM API 批量标注） =====
comments = [
    "这个手机很好用，续航强", "包装破损，退货麻烦", "屏幕显示不错但发热严重",
    "物流很快，好评", "质量太差，差评", "一般般，没什么感觉",
    "性价比高，推荐", "客服态度差", "用了一周就坏了", "外观漂亮，喜欢",
    "功能正常，没什么惊喜", "价格贵，不值", "很好用，会回购", "假货，举报",
    "凑合能用", "超出预期，赞", "信号差，经常断", "整体满意", "客服回复慢",
]
# LLM 标注结果（实际调用 API 获取）
labels = ["正","负","负","正","负","中","正","负","负","正",
          "中","负","正","负","中","正","负","正","负"]

df = pd.DataFrame({"text": comments, "label": labels})
print("数据分布:")
print(df["label"].value_counts())

# ===== 2. 文本转向量（TF-IDF） =====
vectorizer = TfidfVectorizer()
X = vectorizer.fit_transform(df["text"])
y = df["label"]
print(f"\n特征矩阵: {X.shape}")

# ===== 3. 训练分类器 =====
X_tr, X_te, y_tr, y_te = train_test_split(
    X, y, test_size=0.3, random_state=42, stratify=y
)
model = LogisticRegression(max_iter=200)
model.fit(X_tr, y_tr)

# ===== 4. 评估 =====
y_pred = model.predict(X_te)
print("\n分类报告:")
print(classification_report(y_te, y_pred, zero_division=0))

# ===== 5. 预测新评论 =====
new = ["这个产品真不错", "太垃圾了", "还行吧"]
X_new = vectorizer.transform(new)
print("\n新评论预测:")
for text, pred in zip(new, model.predict(X_new)):
    print(f"  {text} → {pred}")

# ===== 6. 项目意义说明 =====
print("""
项目意义：
- LLM 当标注员：小批量数据用 LLM 标注，省人工
- sklearn 当学习者：把 LLM 能力"蒸馏"到轻量模型，部署便宜
- 这是工业界"模型蒸馏"的简化版，是 LLM 时代常见模式
""")
