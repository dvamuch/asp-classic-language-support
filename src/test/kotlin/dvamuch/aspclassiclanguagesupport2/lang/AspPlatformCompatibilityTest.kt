package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** Prevents a green formatter audit from silently using a different IDE SDK. */
class AspPlatformCompatibilityTest : BasePlatformTestCase() {
    fun testRunsOnSupportedPhpStormBuild() {
        val info = ApplicationInfo.getInstance()
        val expectedBuild = System.getProperty("asp.expected.platform.build", "252.27397.112")
        assertEquals("Tests must use the explicitly selected PhpStorm build", expectedBuild, info.build.asStringWithoutProductCode())
        assertEquals("PS", info.build.productCode)
        println("ASP test platform: ${info.fullVersion}, ${info.build.asString()}")
    }
}
