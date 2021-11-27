package xyz.fmdc.arw.modcore.baseclass.module.rotatable

open class ModuleRotatablePitch {
    private var _pitchDeg: Double = 0.0
    var pitchDeg: Double
        get() = _pitchDeg
        set(value) {
            _pitchDeg = value
            _pitchRad = Math.toRadians(value)
        }

    private var _pitchRad: Double = 0.0
    var pitchRad: Double
        get() = _pitchRad
        set(value) {
            _pitchRad = value
            _pitchDeg = Math.toDegrees(value)
        }
}
