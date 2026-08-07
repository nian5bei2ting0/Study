# Day2 示例：线性模型家族
# 运行：python code/Day2_linear_models.py
from sklearn.datasets import fetch_california_housing, load_breast_cancer
from sklearn.linear_model import LinearRegression, Ridge, Lasso, LogisticRegression
from sklearn.svm import SVC, LinearSVC
from sklearn.preprocessing import PolynomialFeatures, StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report
import matplotlib.pyplot as plt

# 回归
X, y = fetch_california_housing(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)

for name, m in [("LR", LinearRegression()), ("Ridge", Ridge(1.0)), ("Lasso", Lasso(0.1))]:
    m.fit(X_tr, y_tr)
    print(f"{name}: R²={m.score(X_te, y_te):.4f}")

# Ridge alpha 调参
alphas = [0.01, 0.1, 1, 10, 100]
scores = [Ridge(a).fit(X_tr, y_tr).score(X_te, y_te) for a in alphas]
plt.plot(alphas, scores, marker="o"); plt.xscale("log")
plt.xlabel("alpha"); plt.ylabel("R²"); plt.title("Ridge 调参"); plt.show()

# Lasso 稀疏
l = Lasso(0.1).fit(X_tr, y_tr)
print("Lasso 非零系数:", (l.coef_ != 0).sum())

# 多项式特征
pipe = Pipeline([("poly", PolynomialFeatures(2)), ("lr", LinearRegression())])
pipe.fit(X_tr, y_tr)
print("带多项式 R²:", pipe.score(X_te, y_te))

# 分类
X, y = load_breast_cancer(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

for cw in [None, "balanced"]:
    m = Pipeline([("sc", StandardScaler()),
                  ("lr", LogisticRegression(class_weight=cw, max_iter=5000))]).fit(X_tr, y_tr)
    print(f"\nclass_weight={cw}")
    print(classification_report(y_te, m.predict(X_te)))

# SVM
svm_lin = Pipeline([("sc", StandardScaler()), ("m", LinearSVC(C=1.0, max_iter=10000))]).fit(X_tr, y_tr)
svm_rbf = Pipeline([("sc", StandardScaler()), ("m", SVC(kernel="rbf", C=1.0, gamma="scale"))]).fit(X_tr, y_tr)
print("LinearSVC:", svm_lin.score(X_te, y_te))
print("RBF SVM:", svm_rbf.score(X_te, y_te))
