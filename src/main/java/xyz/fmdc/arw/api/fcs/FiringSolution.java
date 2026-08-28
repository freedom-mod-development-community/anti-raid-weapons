package xyz.fmdc.arw.api.fcs;

public record FiringSolution(
        float targetYaw,
        float targetPitch,
        boolean allowFire,
        boolean isTargetLocked
) {
    public static final FiringSolution IDLE = new FiringSolution(0.0f, 0.0f, false, false);
}
