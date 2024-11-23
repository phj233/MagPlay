package top.phj233.magplay.ui.screens.work.caculate

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import top.phj233.magplay.entity.Calculator
import top.phj233.magplay.nav.LocalNavController
import top.phj233.magplay.ui.components.CalculateUserDetailCard
import top.phj233.magplay.ui.components.MagPlayTopBar
import top.phj233.magplay.ui.screens.work.CalculatorViewModel
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun Calculate() {
    val nav = LocalNavController.current
    val viewModel: CalculatorViewModel = viewModel()
    val calculators by viewModel.calculators.collectAsState()
    var sexSelect by remember { mutableStateOf(false) }
    var userWeight by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    val brush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color.Transparent
        )
    )

    Scaffold(
        topBar = {
            MagPlayTopBar("身高体重计算器")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("请输入你的名字:", fontSize = 20.sp)
                BasicTextField(
                    value = userName,
                    onValueChange = {
                        userName = it
                    },
                    textStyle = TextStyle(fontSize = 16.sp),
                    modifier = Modifier
                        .width(78.dp)
                        .height(24.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
                    // 添加边框
                    decorationBox = { innerTextField ->
                        // 不添加任何 padding
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            innerTextField()
                        }

                    }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("请输入你的体重:", fontSize = 20.sp)
                BasicTextField(
                    value = userWeight,
                    onValueChange = { userWeight = it },
                    textStyle = TextStyle(fontSize = 16.sp),
                    modifier = Modifier
                        .width(50.dp)
                        .height(24.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    ),
                    // 添加边框
                    decorationBox = { innerTextField ->
                        // 不添加任何 padding
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            innerTextField()
                        }

                    }
                )
                Text(" kg")

            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("请选择你的性别:", fontSize = 20.sp)
                Text("男", fontSize = 16.sp)
                RadioButton(
                    selected = sexSelect,
                    onClick = {
                        sexSelect = !sexSelect
                    },
                    modifier = Modifier
                )
                Text("女", fontSize = 16.sp)
                RadioButton(
                    selected = !sexSelect,
                    onClick = {
                        sexSelect = !sexSelect
                    }
                )


            }
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        if (userName.isEmpty() || userWeight.isEmpty()) {
                            return@Button
                        }
                        showDialog = true
                    },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text("计算")
                }
            }

            // 当前位置展示所有的数据
            LazyColumn(
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(calculators.reversed()) { calculator ->
                    CalculateUserDetailCard(calculator, onDelete = {
                        viewModel.deleteCalculator(it)
                    })
                }
            }

            if (showDialog) {
                if (userName.isEmpty() || userWeight.isEmpty()) {
                    return@Column
                }
                Calculator(
                    uid = null,
                    name = userName,
                    sex = if (sexSelect) "男" else "女",
                    weight = userWeight.toDouble(),
                    height = calculate(userWeight, sexSelect).toDouble(),
                    createTime = LocalDateTime.now().toString()
                ).let {
                    viewModel.insertCalculator(it)
                }
                Dialog(
                    onDismissRequest = {
                        showDialog = false
                    },
                ) {
                    Card(
                        modifier = Modifier
                            .height(200.dp)
                            .width(200.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("${userName}你的标准身高是:")
                            Text("${calculate(userWeight, sexSelect)} cm")
                        }
                    }
                }
            }
        }
    }
}

fun calculate(userWeight:String,sexSelect:Boolean): String {
    if (userWeight.isEmpty()){
        throw Exception("体重不能为空")
    }
    val weight = userWeight.toDouble()
    var height: Int = 0
    //男性(身高cm-80)x70%=标准体重(kg)，女性(身高cm-70)x60%=标准体重(kg)。
    height = if (sexSelect){
        (weight/0.7+80).toInt()
    } else{
        (weight/0.6+70).toInt()
    }
    return height.toString()

}