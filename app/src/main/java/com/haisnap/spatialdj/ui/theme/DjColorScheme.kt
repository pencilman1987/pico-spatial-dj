package com.haisnap.spatialdj.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.haisnap.spatialdj.R
import com.pico.spatial.ui.design.ColorScheme
import com.pico.spatial.ui.design.systemColorScheme

@Composable
fun rememberDjColorScheme(): ColorScheme {
    val base = systemColorScheme(LocalContext.current)
    return base.copy(
        fillPrimary = colorResource(R.color.dj_carbon),
        fillSecondary = colorResource(R.color.dj_gunmetal),
        fillTertiary = colorResource(R.color.dj_panel),
        fillLight = colorResource(R.color.dj_panel_light),
        labelPrimaryLight = colorResource(R.color.dj_ink),
        labelPrimary = colorResource(R.color.dj_steel),
        labelSecondary = colorResource(R.color.dj_label_secondary),
        labelTertiary = colorResource(R.color.dj_label_tertiary),
        labelQuaternary = colorResource(R.color.dj_label_quaternary),
        lightenHover = colorResource(R.color.dj_hover),
        lightenPressed = colorResource(R.color.dj_pressed),
        error = colorResource(R.color.dj_on_air_red),
        alert = colorResource(R.color.dj_signal_amber),
        passable = colorResource(R.color.dj_meter_green),
        interaction = colorResource(R.color.dj_cue_cyan),
        dividerLine = colorResource(R.color.dj_divider),
    )
}
