# Day7 综合实战：信用卡欺诈检测（端到端 ML 项目）
# 运行：python code/Day7_project.py
# 详细讲解见 Day7_综合实战.md
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import joblib

from sklearn.datasets import make_classification
from sklearn.model_selection import (train_test_split, StratifiedKFold,
                                       cross_validate, GridSearchCV)
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier, HistGradientBoostingClassifier
from sklearn.naive_bayes import GaussianNB
from sklearn.neighbors import KNeighborsClassifier
from sklearn.metrics import (classification_report, confusion_matrix,
                              roc_curve, auc, RocCurveDisplay,
                              precision_recall_curve, average_precision_score,
                              PrecisionRecallDisplay)

plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False

# 1. 生成不平衡数据
X, y = make_classification(n_samples=5000, n_features=20, n_informative=10,
                            n_redundant=5, n_classes=2, weights=[0.95, 0.05],
                            random_state=42)
print(f"样本数: {len(y)}, 欺诈占比: {y.mean():.2%}")

# 2. EDA
df = pd.DataFrame(X, columns=[f"f{i}" for i in range(20)])
df["label"] = y
print(df["label"].value_counts())
df["label"].value_counts().plot.bar(); plt.title("类别分布"); plt.show()

# 3. 划分
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2,
                                            random_state=42, stratify=y)

# 4. 多模型对比
models = {
    "LogReg": Pipeline([("sc", StandardScaler()),
                         ("m", LogisticRegression(class_weight="balanced", max_iter=5000))]),
    "RF": RandomForestClassifier(100, class_weight="balanced", random_state=42, n_jobs=-1),
    "HistGBDT": HistGradientBoostingClassifier(random_state=42),
    "NB": GaussianNB(),
    "KNN": Pipeline([("sc", StandardScaler()), ("m", KNeighborsClassifier(5))]),
}
cv = StratifiedKFold(5, shuffle=True, random_state=42)
results = []
for name, m in models.items():
    r = cross_validate(m, X_tr, y_tr, cv=cv,
                       scoring=["precision","recall","f1","roc_auc"])
    results.append({"model": name, "f1": r["test_f1"].mean(),
                     "recall": r["test_recall"].mean(),
                     "precision": r["test_precision"].mean(),
                     "auc": r["test_roc_auc"].mean()})
print("\n模型对比:")
print(pd.DataFrame(results).sort_values("f1", ascending=False))

# 5. 调参 HistGBDT
gs = GridSearchCV(HistGradientBoostingClassifier(random_state=42),
                  {"max_iter":[100,200], "learning_rate":[0.05,0.1,0.2],
                   "max_depth":[3,5,None]},
                  cv=cv, scoring="f1", n_jobs=-1).fit(X_tr, y_tr)
print(f"\n最佳参数: {gs.best_params_}")
print(f"最佳 CV F1: {gs.best_score_:.4f}")

# 6. 测试集评估
final = gs.best_estimator_
y_pred = final.predict(X_te)
y_score = final.predict_proba(X_te)[:, 1]
print("\n测试集分类报告:")
print(classification_report(y_te, y_pred))
print("混淆矩阵:\n", confusion_matrix(y_te, y_pred))

# 7. ROC 与 PR
fpr, tpr, _ = roc_curve(y_te, y_score)
print(f"AUC: {auc(fpr, tpr):.4f}")
RocCurveDisplay.from_predictions(y_te, y_score); plt.show()

prec, rec, thr = precision_recall_curve(y_te, y_score)
print(f"AP: {average_precision_score(y_te, y_score):.4f}")
PrecisionRecallDisplay.from_predictions(y_te, y_score); plt.show()

# 8. 阈值调优：召回 ≥ 0.9
target_recall = 0.9
idx = next(i for i, r in enumerate(rec) if r >= target_recall)
best_thr = thr[idx] if idx < len(thr) else 0.5
y_custom = (y_score >= best_thr).astype(int)
print(f"\n阈值 {best_thr:.3f} 下（召回≥{target_recall}）:")
print(classification_report(y_te, y_custom))

# 9. 保存模型
joblib.dump({"model": final, "threshold": best_thr}, "fraud_model.pkl")
print("\n模型已保存 fraud_model.pkl")
