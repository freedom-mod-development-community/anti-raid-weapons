package xyz.fmdc.arw.common.entity.projectile;

public enum NavalAmmoType {
    MK68_HE_CVT("Mark 68 HE-CVT", 31.1f, 0.663f, 3.7f, AmmoCategory.HE_PROXIMITY),
    MK80_HE_PD("Mark 80 HE-PD", 30.7f, 0.660f, 3.7f, AmmoCategory.HE_POINT_DETONATING),
    MK91_ILLUM_MT("Mark 91 Illum-MT", 29.0f, 0.663f, 0.0f, AmmoCategory.ILLUMINATION),
    MK116_HE_VT("Mark 116 HE-VT", 31.6f, 0.660f, 3.7f, AmmoCategory.HE_PROXIMITY),
    MK127_HE_CVT("Mark 127 HE-CVT", 31.1f, 0.660f, 3.7f, AmmoCategory.HE_PROXIMITY),
    MK156_HE_IR("Mark 156 HE-IR", 31.3f, 0.660f, 3.7f, AmmoCategory.HE_INFRARED),
    MK172_HE_ICM("Mark 172 HE-ICM", 31.0f, 0.660f, 1.5f, AmmoCategory.CARGO_SUBMUNITION);

    private final String name;
    private final float weightKg;
    private final float lengthMeters;
    private final float explosiveFillerKg;
    private final AmmoCategory category;

    NavalAmmoType(String name, float weightKg, float lengthMeters, float explosiveFillerKg, AmmoCategory category) {
        this.name = name;
        this.weightKg = weightKg;
        this.lengthMeters = lengthMeters;
        this.explosiveFillerKg = explosiveFillerKg;
        this.category = category;
    }

    public String getName() { return name; }
    public float getWeightKg() { return weightKg; }
    public float getLengthMeters() { return lengthMeters; }
    public float getExplosiveFillerKg() { return explosiveFillerKg; }
    public AmmoCategory getCategory() { return category; }

    public enum AmmoCategory {
        HE_POINT_DETONATING, // 瞬発信管高爆薬
        HE_PROXIMITY,         // 近接信管高爆薬
        ILLUMINATION,         // 照明弾
        HE_INFRARED,          // 赤外線信管
        CARGO_SUBMUNITION     // 子弾散布（ICM）
    }
}
