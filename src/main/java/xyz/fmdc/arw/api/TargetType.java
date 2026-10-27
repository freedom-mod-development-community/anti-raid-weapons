package xyz.fmdc.arw.api;

/**
 * レーダー探知目標の種別（エンティティ、標的ブロック、仮想飛翔体など）。
 */
public enum TargetType {
    UNKNOWN,
    ENTITY,
    BLOCK,
    PROJECTILE,
    MISSILE,
    SHELL
}
