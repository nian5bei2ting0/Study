# Day6 示例：模型评估深入
# 运行：python code/Day6_evaluation.py
from sklearn.datasets import load_breast_cancer
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.neighbors import KNeighborsClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.model_selection import (train_test_split, StratifiedKFold,
                                       cross_validate, learning_curve, validation_curve)
from sklearn.metrics import (classification_report, roc_curve, auc, RocCurveDisplay,
                              precision_recall_curve, average_precision_score,
                              PrecisionRecallDisplay)
import numpy as np, matplotlib.pyplot as plt

plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False

X, y = load_breast_cancer(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

# 多指标 CV
lr = Pipeline([("sc",StandardScaler()),("m",LogisticRegression(max_iter=5000))])
cv = StratifiedKFold(5, shuffle=True, random_state=42)
r = cross_validate(lr, X, y, cv=cv,
                    scoring=["accuracy","precision","recall","f1","roc_auc"])
import pandas as pd
print("多指标 CV:")
print(pd.DataFrame({k:v for k,v in r.items() if k.startswith("test")}))

# ROC
lr.fit(X_tr, y_tr)
y_score = lr.predict_proba(X_te)[:,1]
fpr, tpr, _ = roc_curve(y_te, y_score)
print(f"\nAUC: {auc(fpr, tpr):.4f}")
RocCurveDisplay.from_predictions(y_te, y_score)
plt.plot([0,1],[0,1],"--",color="gray")
plt.title("ROC"); plt.show()

# PR
prec, rec, thr = precision_recall_curve(y_te, y_score)
print(f"AP: {average_precision_score(y_te, y_score):.4f}")
PrecisionRecallDisplay.from_predictions(y_te, y_score)
plt.title("PR"); plt.show()

# 阈值调优：召回 ≥ 0.95
idx = next(i for i,r in enumerate(rec) if r >= 0.95)
best_thr = thr[idx] if idx < len(thr) else 0.5
y_custom = (y_score >= best_thr).astype(int)
print(f"\n阈值 {best_thr:.3f} 下:")
print(classification_report(y_te, y_custom))

# 学习曲线
ts, tr_s, te_s = learning_curve(RandomForestClassifier(random_state=42), X, y, cv=5,
                                  train_sizes=np.linspace(0.1,1.0,10))
plt.plot(ts, tr_s.mean(axis=1), label="train")
plt.plot(ts, te_s.mean(axis=1), label="test")
plt.xlabel("样本数"); plt.legend(); plt.title("学习曲线"); plt.show()

# 验证曲线找最佳 K
pr = range(1, 20)
tr_s, te_s = validation_curve(Pipeline([("sc",StandardScaler()),
                                          ("m",KNeighborsClassifier())]),
                              X, y, param_name="kneighborsclassifier__n_neighbors",
                              param_range=pr, cv=5)
plt.plot(pr, tr_s.mean(axis=1), label="train")
plt.plot(pr, te_s.mean(axis=1), label="test")
plt.xlabel("k"); plt.legend(); plt.title("验证曲线"); plt.show()
