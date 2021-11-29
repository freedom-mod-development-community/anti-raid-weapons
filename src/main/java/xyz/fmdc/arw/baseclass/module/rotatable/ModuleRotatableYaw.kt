package xyz.fmdc.arw.baseclass.module.rotatable

class ModuleRotatableYaw {
    var yawDeg: Double = 0.0
}

var IRotatableYaw.yawDeg: Double
    get() = moduleRotatableYaw.yawDeg
    set(value) {
        moduleRotatableYaw.yawDeg = value
    }