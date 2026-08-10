# -*- coding: utf-8 -*-
"""
Day6 · 回归分析基础
目标：用线性/逻辑回归做预测与解释，解读系数和R²
依赖：pip install numpy pandas statsmodels scikit-learn matplotlib
"""
import numpy as np
import pandas as pd
import statsmodels.api as sm
import matplotlib.pyplot as plt
from sklearn.linear_model import LogisticRegression
from sklearn.datasets import make_classification
from sklearn.metrics import r2_score

plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei']
plt.rcParams['axes.unicode_minus'] = False


def linear_regression():
    """线性回归：用 statsmodels 看完整统计推断"""
    np.random.seed(42)
    # 广告投入 vs 销售额
    ad = np.random.uniform(10, 100, 200)
    sales = 50 + 3 * ad + np.random.normal(0, 15, 200)

    X = sm.add_constant(ad)
    model = sm.OLS(sales, X).fit()

    print("=" * 50)
    print("线性回归：广告投入 vs 销售额")
    print("=" * 50)
    print(model.summary())

    # 关键解读
    print("\n关键解读：")
    print(f"  R² = {model.rsquared:.3f}（模型解释了{model.rsquared:.1%}的变异）")
    print(f"  截距 = {model.params[0]:.2f}（无广告时销售额）")
    print(f"  广告系数 = {model.params[1]:.2f}（广告每增1，销售额增{model.params[1]:.2f}）")
    print(f"  系数p值 = {model.pvalues[1]:.4f}（{'显著' if model.pvalues[1] < 0.05 else '不显著'}）")

    # 画拟合线
    plt.figure(figsize=(8, 5))
    plt.scatter(ad, sales, alpha=0.5)
    plt.plot(sorted(ad), model.predict(sm.add_constant(sorted(ad))), 'r-', lw=2)
    plt.xlabel('广告投入'); plt.ylabel('销售额')
    plt.title(f'线性回归 R²={model.rsquared:.3f}')
    plt.savefig('day6_linear.png', dpi=100, bbox_inches='tight')
    plt.show()


def residual_analysis():
    """残差分析：检查回归假设"""
    np.random.seed(42)
    x = np.random.uniform(0, 100, 200)
    y = 2 + 3 * x + np.random.normal(0, 10, 200)
    X = sm.add_constant(x)
    model = sm.OLS(y, X).fit()
    residuals = model.resid
    fitted = model.fittedvalues

    fig, axes = plt.subplots(1, 2, figsize=(12, 4))

    # 残差 vs 拟合值（看是否随机散布）
    axes[0].scatter(fitted, residuals, alpha=0.5)
    axes[0].axhline(0, color='r', linestyle='--')
    axes[0].set_xlabel('拟合值'); axes[0].set_ylabel('残差')
    axes[0].set_title('残差图（随机散布=假设成立）')

    # Q-Q图（残差正态性）
    from scipy import stats
    stats.probplot(residuals, dist='norm', plot=axes[1])
    axes[1].set_title('残差Q-Q图（点在直线上=正态）')

    plt.savefig('day6_residual.png', dpi=100, bbox_inches='tight')
    plt.show()

    print("[残差分析] 随机散布 + Q-Q点在直线上 → 假设成立")


def logistic_regression():
    """逻辑回归：Y是二分类"""
    np.random.seed(42)
    X, y = make_classification(n_samples=500, n_features=3,
                                n_informative=3, n_redundant=0,
                                random_state=42)

    model = LogisticRegression().fit(X, y)
    print("=" * 50)
    print("逻辑回归：用户特征 vs 是否流失")
    print("=" * 50)
    print(f"系数: {model.coef_}")
    print(f"截距: {model.intercept_}")

    # 概率预测
    probs = model.predict_proba(X[:5])[:, 1]
    print(f"前5个用户流失概率: {probs}")

    # 系数解读（odds ratio）
    print("\n系数解读（odds ratio = e^系数）:")
    for i, coef in enumerate(model.coef_[0]):
        odds_ratio = np.exp(coef)
        direction = "增加" if coef > 0 else "减少"
        print(f"  特征{i}: 系数={coef:.3f}, OR={odds_ratio:.3f}（X增1，流失几率{direction}{(odds_ratio-1):.1%}）")


def multicollinearity():
    """多重共线性检测：VIF"""
    np.random.seed(42)
    n = 200
    x1 = np.random.normal(0, 1, n)
    x2 = x1 * 0.9 + np.random.normal(0, 0.1, n)  # 与x1高度相关
    x3 = np.random.normal(0, 1, n)               # 独立
    y = 1 + 2*x1 + 3*x3 + np.random.normal(0, 1, n)

    df = pd.DataFrame({'x1': x1, 'x2': x2, 'x3': x3, 'y': y})
    print("=" * 50)
    print("多重共线性检测")
    print("=" * 50)
    print("相关性矩阵：")
    print(df[['x1','x2','x3']].corr())
    print(f"\nx1 vs x2 相关: {df['x1'].corr(df['x2']):.3f}（>0.8 高度共线）")

    # VIF
    from statsmodels.stats.outliers_influence import variance_inflation_factor
    X = df[['x1','x2','x3']]
    X_const = sm.add_constant(X)
    print("\nVIF（方差膨胀因子，>5有问题，>10严重）:")
    for i, col in enumerate(['x1','x2','x3']):
        vif = variance_inflation_factor(X_const.values, i+1)
        print(f"  {col}: VIF={vif:.2f}")
    print("→ x1, x2 VIF很高，应删一个")


if __name__ == '__main__':
    linear_regression()
    print()
    residual_analysis()
    print()
    logistic_regression()
    print()
    multicollinearity()
