# -*- coding: utf-8 -*-
"""
Day1 · 概率与分布
目标：用代码理解四大分布，学会判断数据服从什么分布
依赖：pip install numpy scipy matplotlib
"""
import numpy as np
import matplotlib.pyplot as plt
from scipy import stats

# 解决中文显示
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei']
plt.rcParams['axes.unicode_minus'] = False


def demo_normal():
    """正态分布：钟形对称，68-95-99.7 法则"""
    mu, sigma = 100, 15  # 均值100，标准差15（如智商）
    data = np.random.normal(mu, sigma, 10000)

    plt.figure(figsize=(10, 4))
    plt.hist(data, bins=50, density=True, alpha=0.6, label='实际数据')

    # 理论密度曲线
    x = np.linspace(40, 160, 200)
    plt.plot(x, stats.norm.pdf(x, mu, sigma), 'r-', lw=2, label='理论密度')

    # 68-95-99.7 法则标注
    for k, c in [(1, 'green'), (2, 'orange'), (3, 'red')]:
        plt.axvline(mu + k * sigma, color=c, linestyle='--', alpha=0.5)
        plt.axvline(mu - k * sigma, color=c, linestyle='--', alpha=0.5)

    plt.title(f'正态分布 μ={mu}, σ={sigma}')
    plt.legend()
    plt.savefig('day1_normal.png', dpi=100, bbox_inches='tight')
    plt.show()

    # z-score 与异常检测
    z = (data - mu) / sigma
    outliers = data[np.abs(z) > 3]
    print(f"[正态] 异常值(|z|>3)数量: {len(outliers)}/{len(data)}")
    print(f"[正态] ±1σ 覆盖率: {np.mean(np.abs(z) <= 1):.1%} (理论68%)")
    print(f"[正态] ±2σ 覆盖率: {np.mean(np.abs(z) <= 2):.1%} (理论95%)")


def demo_binomial():
    """二项分布：n 次独立试验，成功几次"""
    n, p = 100, 0.1  # 100个用户，转化率10%
    data = np.random.binomial(n, p, 10000)

    plt.figure(figsize=(10, 4))
    plt.hist(data, bins=30, density=True, alpha=0.6)
    plt.title(f'二项分布 n={n}, p={p}（期望={n*p}）')
    plt.savefig('day1_binomial.png', dpi=100, bbox_inches='tight')
    plt.show()

    # 实际100个用户来了20个转化，正常吗？
    prob = 1 - stats.binom.cdf(19, n, p)
    print(f"[二项] 100人转化≥20的概率: {prob:.4f}（很低→可能异常）")


def demo_poisson():
    """泊松分布：单位时间事件次数"""
    lam = 5  # 每分钟平均5个电话
    data = np.random.poisson(lam, 10000)

    plt.figure(figsize=(10, 4))
    plt.hist(data, bins=range(0, 20), density=True, alpha=0.6)
    plt.title(f'泊松分布 λ={lam}（期望=方差={lam}）')
    plt.savefig('day1_poisson.png', dpi=100, bbox_inches='tight')
    plt.show()

    # 某分钟来了10个电话，异常吗？
    prob = 1 - stats.poisson.cdf(9, lam)
    print(f"[泊松] 每分钟≥10个电话的概率: {prob:.4f}")


def demo_lognormal():
    """对数正态分布：金额、收入等长尾数据"""
    amounts = np.random.lognormal(3, 1, 10000)  # 取log后均值3、标准差1

    fig, axes = plt.subplots(1, 2, figsize=(12, 4))

    # 原始：长尾
    axes[0].hist(amounts, bins=50, density=True, alpha=0.6)
    axes[0].axvline(amounts.mean(), color='r', label=f'均值={amounts.mean():.0f}')
    axes[0].axvline(np.median(amounts), color='g', label=f'中位数={np.median(amounts):.0f}')
    axes[0].set_title('金额分布（长尾，均值>>中位数）')
    axes[0].legend()

    # 取对数后：近似正态
    axes[1].hist(np.log(amounts), bins=50, density=True, alpha=0.6)
    axes[1].set_title('取对数后（近似正态）')

    plt.savefig('day1_lognormal.png', dpi=100, bbox_inches='tight')
    plt.show()

    print(f"[对数正态] 均值={amounts.mean():.0f}, 中位数={np.median(amounts):.0f}")
    print(f"[对数正态] 偏度={stats.skew(amounts):.2f}（>0正偏）")
    print(f"[对数正态] 取对数后偏度={stats.skew(np.log(amounts)):.2f}（趋近0）")


def judge_distribution():
    """判断数据服从什么分布：直方图 + QQ图"""
    # 模拟一份金额数据
    data = np.random.lognormal(3, 1, 2000)

    fig, axes = plt.subplots(1, 2, figsize=(12, 4))

    # 直方图
    axes[0].hist(data, bins=50, density=True, alpha=0.6)
    axes[0].set_title('直方图（看形状）')

    # QQ图：与正态对比
    stats.probplot(data, dist='norm', plot=axes[1])
    axes[1].set_title('QQ图（vs 正态）')

    plt.savefig('day1_qq.png', dpi=100, bbox_inches='tight')
    plt.show()

    # 点偏离直线 = 不符合正态
    print("[QQ图] 点偏离直线 → 不服从正态（金额数据常见）")

    # 试对数后
    fig, ax = plt.subplots(figsize=(6, 4))
    stats.probplot(np.log(data), dist='norm', plot=ax)
    ax.set_title('取对数后QQ图')
    plt.savefig('day1_qq_log.png', dpi=100, bbox_inches='tight')
    plt.show()
    print("[对数后QQ图] 点更接近直线 → 取对数后近似正态")


if __name__ == '__main__':
    print("=" * 50)
    print("1. 正态分布")
    demo_normal()
    print("\n2. 二项分布")
    demo_binomial()
    print("\n3. 泊松分布")
    demo_poisson()
    print("\n4. 对数正态分布")
    demo_lognormal()
    print("\n5. 判断分布（QQ图）")
    judge_distribution()
