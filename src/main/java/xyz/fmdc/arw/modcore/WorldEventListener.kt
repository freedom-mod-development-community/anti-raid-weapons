package xyz.fmdc.arw.modcore

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.event.world.WorldEvent

object WorldEventListener {
    @SubscribeEvent
    fun worldLoadEvent(event: WorldEvent.Load) {

    }

    @SubscribeEvent
    fun worldUnloadEvent(event: WorldEvent.Unload) {

    }
}