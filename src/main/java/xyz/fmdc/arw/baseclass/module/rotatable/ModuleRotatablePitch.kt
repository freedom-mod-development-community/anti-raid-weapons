package xyz.fmdc.arw.baseclass.module.rotatable

class ModuleRotatablePitch {
    var pitchDeg: Double = 0.0
}

var IRotatablePitch.pitchDeg: Double
    get() = moduleRotatablePitch.pitchDeg
    set(value) {
        moduleRotatablePitch.pitchDeg = value
    }