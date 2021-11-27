package xyz.fmdc.arw.modcore.proxy

import xyz.fmdc.arw.registry.RegistryRenderer

class AWMClientProxy : AWMProxy() {
    override fun init() {
    }

    override fun callRegisterRenderer() {
        RegistryRenderer.registerRenderer()
    }
}
