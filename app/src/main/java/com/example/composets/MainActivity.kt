package com.example.composets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.composets.ui.theme.ComposeTsTheme
import com.example.composets.ui.theme.Grey200
import com.example.composets.ui.theme.Purple200
import com.example.composets.ui.theme.Purple700
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeTsTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    Greeting("开始连接")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        val textTs = remember { mutableStateOf(name) }
        val data = remember { mutableStateOf("data loading...") }
        Column {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(0.dp, 20.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(Color.Cyan)
            ) {
                Text(
                    text = textTs.value,
                    color = Purple200,
                    modifier = Modifier
                        .background(Color.LightGray)
                        .width(190.dp)
                        .height(35.dp)
                        .padding(5.dp)
                        .align(Alignment.CenterHorizontally)
                        .clickable {
                            logger.debug { "------MQTT开始连接------" }
                            textTs.value = "MQTT连接中..."
                        },
                    style = TextStyle(textAlign = TextAlign.Center),
                    fontSize = 16.sp
                )
                Button(modifier = Modifier
                    .padding(0.dp, 20.dp)
                    .align(Alignment.CenterHorizontally), onClick = {
                }) {
                    Text(text = "测试MQTT接口")
                }
            }
            Text(
                text = data.value,
                color = Purple700,
                style = TextStyle(textAlign = TextAlign.Start),
                fontSize = 13.sp,
                modifier = Modifier
                    .background(Grey200)
                    .padding(5.dp)
                    .fillMaxSize()
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComposeTsTheme {
        Greeting("Connect")
    }
}