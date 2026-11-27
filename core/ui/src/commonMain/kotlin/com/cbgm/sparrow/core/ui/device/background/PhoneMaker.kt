package com.cbgm.sparrow.core.ui.device.background

/**
 * Phone makers whose own power manager stops apps in the background even when Android allows them to run, and where
 * their setting is: the system screens to try in order ([settingsScreens], package to activity), which differ between
 * versions of the maker's system.
 */
enum class PhoneMaker(
    val settingsScreens: List<Pair<String, String>>
) {
    XIAOMI(listOf("com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity")),
    HUAWEI(
        listOf(
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity"
        )
    ),
    HONOR(
        listOf(
            "com.hihonor.systemmanager" to "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
        )
    ),
    OPPO(
        listOf(
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
            "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
            "com.oneplus.security" to "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
        )
    ),
    VIVO(
        listOf(
            "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
        )
    ),
    SAMSUNG(listOf("com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity")),
    ASUS(listOf("com.asus.mobilemanager" to "com.asus.mobilemanager.autostart.AutoStartActivity"));

    companion object {
        /** The maker for a device manufacturer name (`Build.MANUFACTURER`); null for phones that leave apps running. */
        fun of(manufacturer: String): PhoneMaker? =
            when (manufacturer.trim().lowercase()) {
                "xiaomi", "redmi", "poco" -> XIAOMI
                "huawei" -> HUAWEI
                "honor" -> HONOR
                "oppo", "realme", "oneplus" -> OPPO
                "vivo", "iqoo" -> VIVO
                "samsung" -> SAMSUNG
                "asus" -> ASUS
                else -> null
            }
    }
}
