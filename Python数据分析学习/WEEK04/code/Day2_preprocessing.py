# Day2 示例：数据预处理与特征工程
# 运行：python code/Day2_preprocessing.py
import numpy as np
from sklearn.impute import SimpleImputer
from sklearn.preprocessing import (StandardScaler, MinMaxScaler, RobustScaler,
                                    OneHotEncoder, LabelEncoder, OrdinalEncoder)
from sklearn.feature_selection import SelectKBest, f_classif
from sklearn.pipeline import Pipeline
from sklearn.neighbors import KNeighborsClassifier
from sklearn.model_selection import train_test_split
from sklearn.datasets import load_iris

# 缺失值
X = np.array([[1, 2, np.nan], [4, np.nan, 6], [7, 8, 9]])
print("均值填充:\n", SimpleImputer(strategy="mean").fit_transform(X))

# 标签编码（仅用于 y）
le = LabelEncoder()
print("LabelEncoder:", le.fit_transform(["猫","狗","鸟","猫"]))

# 独热编码（用于 X）
ohe = OneHotEncoder(sparse_output=False)
print("OneHot:\n", ohe.fit_transform([["北京"],["上海"],["广州"],["北京"]]))

# 有序编码
oe = OrdinalEncoder(categories=[["低","中","高"]])
print("Ordinal:", oe.fit_transform([["低"],["中"],["高"],["中"]]))

# 缩放
X = np.array([[1, 1000], [2, 2000], [3, 3000]], dtype=float)
print("Standard:\n", StandardScaler().fit_transform(X))
print("MinMax:\n", MinMaxScaler().fit_transform(X))
print("Robust:\n", RobustScaler().fit_transform(X))

# 特征选择
X, y = load_iris(return_X_y=True)
X_new = SelectKBest(f_classif, k=2).fit_transform(X, y)
print("选 2 个特征后:", X_new.shape)

# Pipeline
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)
pipe = Pipeline([("sc", StandardScaler()), ("knn", KNeighborsClassifier())])
pipe.fit(X_tr, y_tr)
print("Pipeline 准确率:", pipe.score(X_te, y_te))

# 保存加载
import joblib
joblib.dump(pipe, "model.pkl")
loaded = joblib.load("model.pkl")
print("加载后准确率:", loaded.score(X_te, y_te))
