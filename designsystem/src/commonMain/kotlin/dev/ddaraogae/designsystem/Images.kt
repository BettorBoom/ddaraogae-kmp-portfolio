package dev.ddaraogae.designsystem

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import dev.ddaraogae.designsystem.resources.Res
import dev.ddaraogae.designsystem.resources.legacy_camera
import dev.ddaraogae.designsystem.resources.legacy_google
import dev.ddaraogae.designsystem.resources.legacy_stamp_detail
import org.jetbrains.compose.resources.painterResource

// Original raster art (res/drawable/camera.png, ic_stamp_detail.png, login_google.png). The
// generated Res class is internal to this module, so features draw them through these.

@Composable
fun CameraBadge(modifier: Modifier = Modifier) =
    Image(painterResource(Res.drawable.legacy_camera), contentDescription = null, modifier = modifier)

/** The black paw print of the stamp detail grid, circle cropped like its CircleImageView. */
@Composable
fun StampPaw(modifier: Modifier = Modifier) =
    Image(painterResource(Res.drawable.legacy_stamp_detail), contentDescription = null,
        modifier = modifier.clip(CircleShape))

@Composable
fun GoogleMark(modifier: Modifier = Modifier) =
    Image(painterResource(Res.drawable.legacy_google), contentDescription = null, modifier = modifier)
