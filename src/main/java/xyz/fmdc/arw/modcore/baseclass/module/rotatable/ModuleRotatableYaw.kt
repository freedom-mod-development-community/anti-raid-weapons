package xyz.fmdc.arw.modcore.baseclass.module.rotatable

open class ModuleRotatableYaw {
    private var _yawDeg: Double = 0.0
    var yawDeg: Double
        get() = _yawDeg
        set(value) {
            _yawDeg = value
            _yawRad = Math.toRadians(value)
        }

    private var _yawRad: Double = 0.0
    var yawRad: Double
        get() = _yawRad
        set(value) {
            _yawRad = value
            _yawDeg = Math.toDegrees(value)
        }
}
