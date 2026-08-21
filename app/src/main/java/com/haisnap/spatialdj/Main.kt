package com.haisnap.spatialdj

import com.haisnap.spatialdj.ui.console.DjConsoleScreen
import com.haisnap.spatialdj.ui.theme.rememberDjColorScheme
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope

fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        DefaultWindowContainer {
            PicoTheme(colorScheme = rememberDjColorScheme()) {
                DjConsoleScreen()
            }
        }
    }
