# Day5 示例：模型选择与超参调优
# 运行：python code/Day5_tuning.py
from sklearn.datasets import load_breast_cancer
from sklearn.ensemble import RandomForestClassifier, HistGradientBoostingClassifier
from sklearn.neighbors import KNeighborsClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier
from sklearn.naive_bayes import GaussianNB
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.model_selection import (GridSearchCV, RandomizedSearchCV,
                                       cross_val_score)
from scipy.stats import randint, loguniform
import pandas as pd

X, y = load_breast_cancer(return_X_y=True)

# GridSearchCV 调随机森林
gs = GridSearchCV(RandomForestClassifier(random_state=42),
                  {"n_estimators":[50,100,200], "max_depth":[3,5,8,None]},
                  cv=5, n_jobs=-1).fit(X, y)
print("RF 最佳:", gs.best_params_, gs.best_score_)

# RandomizedSearchCV 调 KNN（Pipeline 内）
pipe = Pipeline([("sc", StandardScaler()), ("knn", KNeighborsClassifier())])
rs = RandomizedSearchCV(pipe,
                        {"knn__n_neighbors":randint(1,20),
                         "knn__weights":["uniform","distance"]},
                        n_iter=15, cv=5, random_state=42, n_jobs=-1).fit(X, y)
print("KNN 最佳:", rs.best_params_, rs.best_score_)

# 多模型对比
models = {"LogReg":Pipeline([("sc",StandardScaler()),("m",LogisticRegression(max_iter=5000))]),
          "Tree":DecisionTreeClassifier(max_depth=5,random_state=42),
          "RF":RandomForestClassifier(100,random_state=42,n_jobs=-1),
          "NB":GaussianNB(),
          "KNN":Pipeline([("sc",StandardScaler()),("m",KNeighborsClassifier(5))])}
res = [{"m":n,"mean":cross_val_score(m,X,y,cv=5).mean(),
        "std":cross_val_score(m,X,y,cv=5).std()} for n,m in models.items()]
print("\n模型对比:")
print(pd.DataFrame(res).sort_values("mean",ascending=False))

# 精调最佳模型
best = GridSearchCV(RandomForestClassifier(random_state=42),
                    {"n_estimators":[80,100,120],"max_depth":[5,8,10]},
                    cv=5, n_jobs=-1).fit(X, y)
print("\n精调:", best.best_params_, best.best_score_)

# 早停
hgb = HistGradientBoostingClassifier(max_iter=500, early_stopping=True,
                                       n_iter_no_change=10, random_state=42).fit(X, y)
print("HistGBDT 实际迭代:", hgb.n_iter_)
