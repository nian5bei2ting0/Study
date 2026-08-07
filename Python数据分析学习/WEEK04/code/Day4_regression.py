# Day4 示例：监督学习-回归与模型评估
# 运行：python code/Day4_regression.py
from sklearn.datasets import fetch_california_housing
from sklearn.model_selection import (train_test_split, cross_val_score,
                                       GridSearchCV, learning_curve)
from sklearn.linear_model import LinearRegression, Ridge
from sklearn.tree import DecisionTreeRegressor
from sklearn.ensemble import RandomForestRegressor
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
import numpy as np
import matplotlib.pyplot as plt

plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False

X, y = fetch_california_housing(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)

# 线性回归
pipe = Pipeline([("sc", StandardScaler()), ("lr", LinearRegression())])
pipe.fit(X_tr, y_tr)
y_pred = pipe.predict(X_te)
print("线性回归:")
print("  R²:", r2_score(y_te, y_pred))
print("  MAE:", mean_absolute_error(y_te, y_pred))
print("  RMSE:", np.sqrt(mean_squared_error(y_te, y_pred)))

# 交叉验证
scores = cross_val_score(pipe, X, y, cv=5, scoring="r2")
print(f"\n5折 CV R²: {scores.mean():.4f} ± {scores.std():.4f}")

# GridSearchCV 找最佳 max_depth
gs = GridSearchCV(DecisionTreeRegressor(random_state=42),
                  {"max_depth": [3, 5, 7, 10, None]}, cv=5, scoring="r2")
gs.fit(X_tr, y_tr)
print("\n决策树最佳参数:", gs.best_params_)
print("最佳 CV 分数:", gs.best_score_)
print("测试集 R²:", gs.score(X_te, y_te))

# 多模型对比
print("\n多模型对比:")
models = {
    "LinearRegression": Pipeline([("sc", StandardScaler()), ("m", LinearRegression())]),
    "Ridge": Pipeline([("sc", StandardScaler()), ("m", Ridge(alpha=1.0))]),
    "DecisionTree": DecisionTreeRegressor(max_depth=5, random_state=42),
    "RandomForest": RandomForestRegressor(n_estimators=50, random_state=42),
}
for name, m in models.items():
    s = cross_val_score(m, X, y, cv=5, scoring="r2")
    print(f"  {name}: {s.mean():.4f} ± {s.std():.4f}")

# 学习曲线
train_sizes, tr_s, te_s = learning_curve(
    pipe, X, y, cv=5, train_sizes=np.linspace(0.1, 1.0, 10), scoring="r2"
)
plt.plot(train_sizes, tr_s.mean(axis=1), label="train")
plt.plot(train_sizes, te_s.mean(axis=1), label="test")
plt.xlabel("样本数"); plt.ylabel("R²"); plt.legend()
plt.title("学习曲线"); plt.show()
