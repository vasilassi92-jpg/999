package com.localstream.app

import com.localstream.app.ui.navigation.BottomNavScreens
import com.localstream.app.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTest {

    @Test
    fun testScreenRoutesAndBottomNavScreens() {
        assertNotNull(Screen.Server.route)
        assertEquals("server", Screen.Server.route)
        assertEquals("files", Screen.Files.route)
        assertEquals("player", Screen.Player.route)
        assertEquals("renderers", Screen.Renderers.route)
        assertEquals("apps", Screen.Apps.route)
        assertEquals("settings", Screen.Settings.route)

        assertEquals(5, BottomNavScreens.size)
        assertTrue(BottomNavScreens.all { it.route.isNotBlank() })
        val routes = BottomNavScreens.map { it.route }
        assertTrue(routes.contains("server"))
        assertTrue(routes.contains("files"))
        assertTrue(routes.contains("player"))
        assertTrue(routes.contains("renderers"))
        assertTrue(routes.contains("apps"))
    }
}
