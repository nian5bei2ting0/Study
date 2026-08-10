# -*- coding: utf-8 -*-
"""
Day3 · 抽样与中心极限定理
目标：用代码验证 CLT，理解样本为什么能推断总体
"""
import numpy as np
import matplotlib.pyplot as plt
from scipy import stats

plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei']
plt.rcParams['axes.unicode_minus'] = False


def demo_clt():
    """演示中心极限定理：总体非正态，样本均值近似正态"""
    # 总体：对数正态（长尾，非正态）
    np.random.seed(42)
    population = np.random.lognormal(3, 1, 1000000)

    fig, axes = plt.subplots(1, 3, figsize=(15, 4))

    # 总体分布（长尾）
    axes[0].hist(population, bins=100, density=True, alpha=0.6)
    axes[0].set_title(f'总体分布（偏度={stats.skew(population):.2f}，长尾）')

    # 不同样本量下，抽1000个样本算均值
    for i, n in enumerate([5, 30, 200]):
        sample_means = [np.random.choice(population, n).mean() for _ in range(2000)]
        axes[1 if n == 30 else (2 if n == 200 else 0)].hist(sample_means, bins=50, density=True, alpha=0.6)
        # 但要画到对应子图
        # 重新画
    plt.close()

    # 重新画清晰版
    fig, axes = plt.subplots(1, 4, figsize=(16, 4))

    axes[0].hist(population, bins=100, density=True, alpha=0.6)
    axes[0].set_title(f'总体（偏度={stats.skew(population):.2f}）')

    for i, n in enumerate([5, 30, 200], start=1):
        sample_means = [np.random.choice(population, n).mean() for _ in range(2000)]
        axes[i].hist(sample_means, bins=50, density=True, alpha=0.6)
        axes[i].set_title(f'样本均值分布 (n={n}, 偏度={stats.skew(sample_means):.2f})')

    plt.suptitle('中心极限定理：n越大，样本均值越接近正态')
    plt.savefig('day3_clt.png', dpi=100, bbox_inches='tight')
    plt.show()

    print("[CLT] 总体长尾，但样本均值随n增大趋近正态")
    print("[CLT] n≥30时近似正态，可用正态工具做推断")


def demo_standard_error():
    """标准误：样本均值的散布程度"""
    population = np.random.normal(100, 20, 1000000)
    sigma = population.std()

    print("=" * 50)
    print("标准误 SE = σ / √n")
    print("=" * 50)
    for n in [10, 100, 1000, 10000]:
        se = sigma / np.sqrt(n)
        sample_means = [np.random.choice(population, n).mean() for _ in range(2000)]
        actual_se = np.std(sample_means)
        print(f"n={n:>5}: 理论SE={se:.3f}, 实际SE={actual_se:.3f}")

    print("\n→ 样本越大，SE越小，估计越准")


def demo_confidence_interval():
    """置信区间：用样本估总体均值的范围"""
    np.random.seed(42)
    population = np.random.normal(100, 20, 1000000)
    true_mean = population.mean()

    print("=" * 50)
    print(f"真实总体均值: {true_mean:.2f}")
    print("=" * 50)

    # 抽一个样本，算95%置信区间
    sample = np.random.choice(population, 500)
    sample_mean = sample.mean()
    se = sample.std() / np.sqrt(len(sample))
    ci = (sample_mean - 1.96 * se, sample_mean + 1.96 * se)

    print(f"样本均值: {sample_mean:.2f}")
    print(f"95%置信区间: [{ci[0]:.2f}, {ci[1]:.2f}]")
    print(f"真值在区间内: {ci[0] < true_mean < ci[1]}")

    # 验证：100次抽样，95%的区间包含真值
    contains = 0
    for _ in range(100):
        s = np.random.choice(population, 500)
        m = s.mean()
        se = s.std() / np.sqrt(500)
        if m - 1.96 * se < true_mean < m + 1.96 * se:
            contains += 1
    print(f"\n100次抽样中，置信区间包含真值的次数: {contains}（理论95）")


def demo_sample_size():
    """样本量计算：n = (z × σ / E)²"""
    sigma = 20  # 总体标准差
    E = 2       # 允许误差
    z = 1.96    # 95%置信

    n = (z * sigma / E) ** 2
    print(f"σ={sigma}, 允许误差E={E}, 95%置信")
    print(f"所需样本量 n = (1.96 × 20 / 2)² = {n:.0f}")

    # 不同误差需求
    print("\n不同允许误差下的样本量：")
    for E in [5, 2, 1, 0.5]:
        n = (z * sigma / E) ** 2
        print(f"  E={E}: n={n:.0f}")


def sampling_bias_warning():
    """抽样偏差的致命性"""
    # 模拟：有偏样本（只抽富人）
    np.random.seed(42)
    population = np.concatenate([
        np.random.normal(50000, 10000, 8000),  # 普通人
        np.random.normal(200000, 50000, 2000),  # 富人
    ])
    true_mean = population.mean()

    # 有偏抽样：只从富人抽
    rich = population[population > 100000]
    biased_sample = np.random.choice(rich, 10000)

    # 随机抽样
    random_sample = np.random.choice(population, 1000)

    print("=" * 50)
    print("抽样偏差演示")
    print("=" * 50)
    print(f"真实总体均值: {true_mean:.0f}")
    print(f"有偏样本(1万人)均值: {biased_sample.mean():.0f}（严重偏离）")
    print(f"随机样本(1千人)均值: {random_sample.mean():.0f}（接近真值）")
    print("\n→ 有偏样本再大也没用，抽样偏差比样本量更致命")


if __name__ == '__main__':
    demo_clt()
    print()
    demo_standard_error()
    print()
    demo_confidence_interval()
    print()
    demo_sample_size()
    print()
    sampling_bias_warning()
