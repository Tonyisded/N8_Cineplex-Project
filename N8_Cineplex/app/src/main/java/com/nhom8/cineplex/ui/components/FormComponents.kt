package com.nhom8.cineplex.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.relocation.*
import androidx.compose.foundation.text.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.nhom8.cineplex.ui.theme.CineplexColors as C
import kotlinx.coroutines.launch

@Composable
fun LabeledField(key: String, label: String, hint: String, value: String, change: (String) -> Unit, errorMessage: String?, enabled: Boolean,
                 focus: FocusRequester, blur: () -> Unit, password: Boolean = false, email: Boolean = false, last: Boolean = false, submit: () -> Unit = {}) {
    var visible by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val bring = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val manager = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(label) }; withStyle(SpanStyle(color = C.Muted)) { append(" *") } })
        BasicTextField(value, change, enabled = enabled, singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(focus).bringIntoViewRequester(bring).onFocusChanged {
                if(it.isFocused) { focused = true; scope.launch { bring.bringIntoView() } }
                else if(focused) { focused = false; blur() }
            }.semantics { contentDescription = label; if(errorMessage != null) error(errorMessage) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = C.Text), cursorBrush = SolidColor(C.Primary),
            visualTransformation = if(password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if(password) KeyboardType.Password else if(email) KeyboardType.Email else KeyboardType.Text,
                imeAction = if(last) ImeAction.Done else ImeAction.Next, autoCorrectEnabled = !password && !email),
            keyboardActions = KeyboardActions(onNext = { manager.moveFocus(FocusDirection.Down) }, onDone = { manager.clearFocus(); submit() }),
            decorationBox = { inner ->
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).background(C.Surface,RoundedCornerShape(12.dp))
                    .border(if(focused) 2.dp else 1.dp, if(errorMessage != null) C.Error else if(focused) C.Primary else C.Line,RoundedCornerShape(12.dp))
                    .padding(start = 16.dp, end = if(password) 4.dp else 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CineplexIcon(if(email) "mail" else if(password) "lock" else "user",Modifier.size(20.dp))
                    Box(Modifier.weight(1f)) { if(value.isEmpty()) Text(hint,color = C.Muted); inner() }
                    if(password) IconButton(onClick = { visible = !visible }, enabled = enabled, modifier = Modifier.size(48.dp).semantics {
                        contentDescription = "${if(visible) "Ẩn" else "Hiện"} ${label.lowercase()}"
                        stateDescription = if(visible) "Đang hiện" else "Đang ẩn"
                    }) { CineplexIcon(if(visible) "eye_off" else "eye") }
                }
            }
        )
        if(errorMessage != null) Text(errorMessage,color = C.Error,modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}
@Composable
fun PrototypeTabs(labels: List<String>, selected: Int, change: (Int) -> Unit, auth: Boolean = false) {
    Column {
        Row(Modifier.fillMaxWidth().selectableGroup(),horizontalArrangement = Arrangement.spacedBy(if(auth) 16.dp else 24.dp)) {
            labels.forEachIndexed { index,label ->
                val active = selected == index
                Column((if(auth) Modifier.weight(1f) else Modifier.width(IntrinsicSize.Max)).heightIn(min = 48.dp).selectable(active,role = Role.Tab,onClick = { change(index) })
                    .background(if(auth && active) C.Raised else androidx.compose.ui.graphics.Color.Transparent,RoundedCornerShape(topStart = 8.dp,topEnd = 8.dp)),horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.heightIn(min = 45.dp).padding(horizontal = if(auth) 8.dp else 0.dp,vertical = 8.dp),contentAlignment = Alignment.Center) { Text(label,color = if(active) C.Primary else C.Muted,fontWeight = FontWeight.SemiBold) }
                    Box(Modifier.fillMaxWidth().height(3.dp).background(if(active) C.Primary else androidx.compose.ui.graphics.Color.Transparent))
                }
            }
        }
        HorizontalDivider(color = C.Line)
    }
}
