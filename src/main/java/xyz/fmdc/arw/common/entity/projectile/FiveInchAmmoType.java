package xyz.fmdc.arw.common.entity.projectile;

public enum FiveInchAmmoType {
    MK68_HE_CVT("five_inch_shell_mk68_he_cvt", "Mark 68 HE-CVT", 31.1f, 0.663f, 3.7f, AmmoCategory.HE_PROXIMITY),
    MK80_HE_PD("five_inch_shell_mk80_he_pd", "Mark 80 HE-PD", 30.7f, 0.660f, 3.7f, AmmoCategory.HE_POINT_DETONATING),
    MK91_ILLUM_MT("five_inch_shell_mk91_illum_mt", "Mark 91 Illum-MT", 29.0f, 0.663f, 0.0f, AmmoCategory.ILLUMINATION),
    MK116_HE_VT("five_inch_shell_mk116_he_vt", "Mark 116 HE-VT", 31.6f, 0.660f, 3.7f, AmmoCategory.HE_PROXIMITY),
    MK127_HE_CVT("five_inch_shell_mk127_he_cvt", "Mark 127 HE-CVT", 31.1f, 0.660f, 3.7f, AmmoCategory.HE_PROXIMITY),
    MK156_HE_IR("five_inch_shell_mk156_he_ir", "Mark 156 HE-IR", 31.3f, 0.660f, 3.7f, AmmoCategory.HE_INFRARED),
    MK172_HE_ICM("five_inch_shell_mk172_he_icm", "Mark 172 HE-ICM", 31.0f, 0.660f, 1.5f, AmmoCategory.CARGO_SUBMUNITION);

    private final String id;
    private final String name;
    private final float weightKg;
    private final float lengthMeters;
    private final float explosiveFillerKg;
    private final AmmoCategory category;

    FiveInchAmmoType(String id, String name, float weightKg, float lengthMeters, float explosiveFillerKg, AmmoCategory category) {
        this.id = id;
        this.name = name;
        this.weightKg = weightKg;
        this.lengthMeters = lengthMeters;
        this.explosiveFillerKg = explosiveFillerKg;
        this.category = category;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public float getWeightKg() { return weightKg; }
    public float getLengthMeters() { return lengthMeters; }
    public float getExplosiveFillerKg() { return explosiveFillerKg; }
    public AmmoCategory getCategory() { return category; }

    public enum AmmoCategory {
        HE_POINT_DETONATING("point_detonating"), // 瞬発信管高爆薬
        HE_PROXIMITY("proximity"),                 // 近接信管高爆薬
        ILLUMINATION("illumination"),             // 照明弾
        HE_INFRARED("infrared"),                  // 赤外線信管
        CARGO_SUBMUNITION("cargo_submunition");   // 子弾散布（ICM）

        private final String key;

        AmmoCategory(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}
