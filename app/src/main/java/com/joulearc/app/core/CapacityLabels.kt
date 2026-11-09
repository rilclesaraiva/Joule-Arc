package com.joulearc.app.core

import com.joulearc.app.R
import com.joulearc.app.data.CapacitySource

/** Short name of where a detected device capacity came from. */
fun CapacitySource.labelRes(): Int = when (this) {
    CapacitySource.POWER_PROFILE -> R.string.capacity_source_profile
    CapacitySource.POWER_PROFILE_XML -> R.string.capacity_source_xml
    CapacitySource.KERNEL -> R.string.capacity_source_kernel
}
