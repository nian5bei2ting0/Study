# -*- coding: utf-8 -*-
"""
Day4 · 假设检验
目标：学会做 t 检验、解读 p 值、判断差异是否真实
"""
import numpy as np
from scipy import stats
import statsmodels.stats.api as sms


def two_sample_ttest():
    """独立样本 t 检验：两组均值差异是否显著"""
    np.random.seed(42)
    # A/B 测试：两组用户消费金额
    group_a = np.random.normal(100, 20, 1000)
    group_b = np.random.normal(105, 20, 1000)  # B组高5元

    print("=" * 50)
    print("独立样本 t 检验")
    print("=" * 50)
    print(f"A组均值: {group_a.mean():.2f}")
    print(f"B组均值: {group_b.mean():.2f}")
    print(f"差异: {group_b.mean() - group_a.mean():.2f}")

    # t 检验
    t_stat, p_value = stats.ttest_ind(group_a, group_b)
    print(f"t统计量: {t_stat:.3f}")
    print(f"p值: {p_value:.4f}")

    # 解读
    alpha = 0.05
    if p_value < alpha:
        print(f"→ p < {alpha}，差异显著，B组确实更高")
    else:
        print(f"→ p >= {alpha}，差异不显著，不足以说B组更高")

    # 置信区间（更全面）
    cm = sms.CompareMeans(sms.DescrStatsW(group_b), sms.DescrStatsW(group_a))
    ci = cm.tconfint_diff()
    print(f"95%置信区间: [{ci[0]:.2f}, {ci[1]:.2f}]")
    print("→ 区间不含0，进一步确认差异显著")


def effect_size_matters():
    """统计显著 ≠ 业务显著"""
    np.random.seed(42)
    # 大样本，微小差异也显著
    big_a = np.random.normal(100, 20, 100000)
    big_b = np.random.normal(100.2, 20, 100000)  # 只差0.2

    t, p = stats.ttest_ind(big_a, big_b)
    print("=" * 50)
    print("统计显著 ≠ 业务显著")
    print("=" * 50)
    print(f"差异: {big_b.mean() - big_a.mean():.3f}（仅0.2元）")
    print(f"p值: {p:.4f}")
    if p < 0.05:
        print("→ 统计显著，但业务上0.2元差异可能无意义")

    # 小样本，大差异也可能不显著
    small_a = np.random.normal(100, 20, 30)
    small_b = np.random.normal(115, 20, 30)  # 差15元
    t2, p2 = stats.ttest_ind(small_a, small_b)
    print(f"\n小样本(30人)差异15元: p={p2:.4f}")
    if p2 > 0.05:
        print("→ 差异大但样本小，不足以判定显著")


def paired_ttest():
    """配对 t 检验：同一组人前后对比"""
    np.random.seed(42)
    # 同一批用户，活动前后消费
    before = np.random.normal(100, 20, 100)
    after = before + np.random.normal(5, 10, 100)  # 平均提升5

    print("=" * 50)
    print("配对 t 检验（前后对比）")
    print("=" * 50)
    t, p = stats.ttest_rel(after, before)
    print(f"前均值: {before.mean():.2f}, 后均值: {after.mean():.2f}")
    print(f"t={t:.3f}, p={p:.4f}")
    print(f"→ {'显著' if p < 0.05 else '不显著'}")


def normality_check():
    """正态性检验：决定用参数还是非参数检验"""
    np.random.seed(42)
    data = np.random.lognormal(3, 1, 500)

    # Shapiro-Wilk 检验（样本量≤5000）
    stat, p = stats.shapiro(data[:500])
    print("=" * 50)
    print("正态性检验")
    print("=" * 50)
    print(f"偏度={stats.skew(data):.2f}")
    print(f"Shapiro p={p:.4f}")
    if p < 0.05:
        print("→ 不服从正态，应该用非参数检验（Mann-Whitney）")

    # 对比：正态数据 vs 非参数检验
    group_a = np.random.lognormal(3, 1, 200)
    group_b = np.random.lognormal(3.2, 1, 200)

    # 参数检验（可能不准）
    t, p_t = stats.ttest_ind(group_a, group_b)
    # 非参数检验
    u, p_u = stats.mannwhitneyu(group_a, group_b)
    print(f"\n长尾数据对比：")
    print(f"  t检验 p={p_t:.4f}")
    print(f"  Mann-Whitney p={p_u:.4f}")
    print("→ 长尾数据用非参数检验更稳健")


def multiple_comparison():
    """多重比较问题：测多个假设时需校正"""
    np.random.seed(42)
    # 20组数据，实际都来自同一分布（无差异）
    print("=" * 50)
    print("多重比较问题")
    print("=" * 50)

    significant_count = 0
    for i in range(20):
        a = np.random.normal(100, 20, 100)
        b = np.random.normal(100, 20, 100)
        _, p = stats.ttest_ind(a, b)
        if p < 0.05:
            significant_count += 1
            print(f"  第{i+1}次检验: p={p:.4f}（假显著！）")

    print(f"\n20次检验中假显著: {significant_count}次")
    print(f"理论期望: 20×0.05=1次")
    print("→ 多重比较需Bonferroni校正: α/检验次数")


if __name__ == '__main__':
    two_sample_ttest()
    print()
    effect_size_matters()
    print()
    paired_ttest()
    print()
    normality_check()
    print()
    multiple_comparison()
