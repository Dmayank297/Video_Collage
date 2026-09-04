package com.example.videocollage.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videocollage.ui.components.dashedBorder

@Composable
fun VideoUploadScreen(onVideoSelected: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onVideoSelected(it) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Video Collage", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Turn your video into a story-style collage", fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .dashedBorder(Color(0xFF7C4DFF))
                .clickable { launcher.launch("video/*") },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF7C4DFF), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("Drop your video here", fontWeight = FontWeight.SemiBold)
                Text("or tap to browse", fontSize = 13.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("MP4 · Up to 8 people detected", fontSize = 12.sp, color = Color.LightGray)
    }
}
