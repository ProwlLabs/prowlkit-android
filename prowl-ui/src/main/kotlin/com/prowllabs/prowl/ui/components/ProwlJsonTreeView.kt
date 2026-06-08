package com.prowllabs.prowl.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prowllabs.prowl.ui.theme.ProwlColors
import org.json.JSONArray
import org.json.JSONObject

private sealed interface JsonNode {
    val label: String
}

private data class JsonObjectNode(
    override val label: String,
    val children: List<Pair<String, JsonNode>>,
) : JsonNode

private data class JsonArrayNode(
    override val label: String,
    val children: List<JsonNode>,
) : JsonNode

private data class JsonLeafNode(
    override val label: String,
    val valueColor: androidx.compose.ui.graphics.Color,
) : JsonNode

@Composable
fun ProwlJsonTreeView(
    json: String,
    modifier: Modifier = Modifier,
) {
    val root = remember(json) { parseJsonTree(json) }
    Column(modifier = modifier.animateContentSize()) {
        when (root) {
            is JsonObjectNode -> root.children.forEach { (key, node) ->
                JsonNodeRow(label = key, node = node, depth = 0)
            }
            is JsonArrayNode -> root.children.forEachIndexed { index, node ->
                JsonNodeRow(label = "[$index]", node = node, depth = 0)
            }
            is JsonLeafNode -> Text(
                text = root.label,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = root.valueColor,
            )
            null -> Text(
                text = json,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun JsonNodeRow(label: String, node: JsonNode, depth: Int) {
    when (node) {
        is JsonObjectNode, is JsonArrayNode -> {
            var expanded by remember { mutableStateOf(depth < 1) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(start = (depth * 12).dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                )
                Text(
                    text = label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ProwlColors.JsonKey,
                )
            }
            if (expanded) {
                when (node) {
                    is JsonObjectNode -> node.children.forEach { (key, child) ->
                        JsonNodeRow(label = key, node = child, depth = depth + 1)
                    }
                    is JsonArrayNode -> node.children.forEachIndexed { index, child ->
                        JsonNodeRow(label = "[$index]", node = child, depth = depth + 1)
                    }
                    else -> Unit
                }
            }
        }
        is JsonLeafNode -> Row(
            modifier = Modifier.padding(start = ((depth + 1) * 12).dp, top = 1.dp, bottom = 1.dp),
        ) {
            Text(
                text = "$label: ",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = ProwlColors.JsonKey,
            )
            Text(
                text = node.label,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = node.valueColor,
            )
        }
    }
}

private fun parseJsonTree(json: String): JsonNode? = runCatching {
    when {
        json.trimStart().startsWith("{") -> jsonObjectNode("", JSONObject(json))
        json.trimStart().startsWith("[") -> jsonArrayNode("", JSONArray(json))
        else -> null
    }
}.getOrNull()

private fun jsonObjectNode(label: String, obj: JSONObject): JsonObjectNode {
    val keys = obj.keys().asSequence().toList()
    return JsonObjectNode(
        label = label,
        children = keys.map { key ->
            key to jsonValueNode(key, obj.get(key))
        },
    )
}

private fun jsonArrayNode(label: String, array: JSONArray): JsonArrayNode =
    JsonArrayNode(
        label = label,
        children = (0 until array.length()).map { index ->
            jsonValueNode("[$index]", array.get(index))
        },
    )

private fun jsonValueNode(label: String, value: Any?): JsonNode = when (value) {
    is JSONObject -> jsonObjectNode(label, value)
    is JSONArray -> jsonArrayNode(label, value)
    is Number -> JsonLeafNode(value.toString(), ProwlColors.JsonNumber)
    is Boolean -> JsonLeafNode(value.toString(), ProwlColors.JsonLiteral)
    JSONObject.NULL -> JsonLeafNode("null", ProwlColors.JsonLiteral)
    else -> JsonLeafNode("\"$value\"", ProwlColors.JsonString)
}
