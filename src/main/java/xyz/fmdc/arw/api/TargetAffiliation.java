package xyz.fmdc.arw.api;

/**
 * レーダー目標の識別状態（IFF: Identification Friend or Foe）
 */
public enum TargetAffiliation {
    UNKNOWN("UNK", 0xFFFFDD00),   // 未識別（イエロー）
    FRIENDLY("FRND", 0xFF00E5FF), // 味方（シアン）
    HOSTILE("HOST", 0xFFFF3333);  // 敵対（レッド）

    private final String shortLabel;
    private final int color;

    TargetAffiliation(String shortLabel, int color) {
        this.shortLabel = shortLabel;
        this.color = color;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public int getColor() {
        return color;
    }

    public TargetAffiliation next() {
        return switch (this) {
            case UNKNOWN -> FRIENDLY;
            case FRIENDLY -> HOSTILE;
            case HOSTILE -> UNKNOWN;
        };
    }

    public static TargetAffiliation fromOrdinal(int ordinal) {
        TargetAffiliation[] values = values();
        if (ordinal >= 0 && ordinal < values.length) {
            return values[ordinal];
        }
        return UNKNOWN;
    }
}
