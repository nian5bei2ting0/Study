# Day4 示例：朴素贝叶斯与 KNN
# 运行：python code/Day4_nb_knn.py
from sklearn.naive_bayes import GaussianNB, MultinomialNB, BernoulliNB
from sklearn.neighbors import KNeighborsClassifier, KNeighborsRegressor
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.datasets import load_iris, fetch_california_housing
from sklearn.model_selection import train_test_split
import matplotlib.pyplot as plt

X, y = load_iris(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

# GaussianNB
print("GaussianNB:", GaussianNB().fit(X_tr, y_tr).score(X_te, y_te))

# KNN 缩放对比
print("KNN 不缩放:", KNeighborsClassifier(5).fit(X_tr, y_tr).score(X_te, y_te))
print("KNN 缩放:", Pipeline([("sc", StandardScaler()),
                              ("k", KNeighborsClassifier(5))]).fit(X_tr, y_tr).score(X_te, y_te))

# K 调参
scores = []
for k in range(1, 21):
    m = Pipeline([("sc", StandardScaler()),
                  ("k", KNeighborsClassifier(k))]).fit(X_tr, y_tr)
    scores.append(m.score(X_te, y_te))
plt.plot(range(1, 21), scores, marker="o")
plt.xlabel("k"); plt.ylabel("准确率"); plt.title("K 调参"); plt.show()

# weights 对比
for w in ["uniform", "distance"]:
    m = Pipeline([("sc", StandardScaler()),
                  ("k", KNeighborsClassifier(5, weights=w))]).fit(X_tr, y_tr)
    print(f"weights={w}: {m.score(X_te, y_te):.4f}")

# 文本分类
texts = ["免费领奖", "明天开会", "中奖快来", "项目周报", "限时优惠",
         "会议纪要", "免费体验", "下班路上", "点击领取", "晚饭吃啥"]
labels = ["垃圾","正常","垃圾","正常","垃圾","正常","垃圾","正常","垃圾","正常"]
pipe = Pipeline([("v", CountVectorizer()), ("c", MultinomialNB())]).fit(texts, labels)
print("新文本预测:", pipe.predict(["免费中奖", "周报"]))

# KNN 回归
X, y = fetch_california_housing(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)
knn_r = Pipeline([("sc", StandardScaler()),
                  ("k", KNeighborsRegressor(5, weights="distance"))]).fit(X_tr, y_tr)
print("KNN 回归 R²:", knn_r.score(X_te, y_te))
