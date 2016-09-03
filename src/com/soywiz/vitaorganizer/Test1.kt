package com.soywiz.vitaorganizer

object Test1 {
    @JvmStatic fun main(args: Array<String>) {
        PsvitaDevice.setIp("192.168.1.130")
        for (gameId in PsvitaDevice.getGameIds()) {
            val paramSfoFile = VitaOrganizerCache.getParamSfoFile(gameId)
            if (!paramSfoFile.exists()) {
                paramSfoFile.writeBytes(PsvitaDevice.getParamSfo(gameId))
            }

            val iconFile = VitaOrganizerCache.getIcon0File(gameId)
            if (!iconFile.exists()) {
                iconFile.writeBytes(PsvitaDevice.getGameIcon(gameId))
            }
        }
    }
}