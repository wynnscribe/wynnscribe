package com.wynnscribe.schemas

import com.wynnscribe.schemas.LogicalOperator.*
import com.wynnscribe.utils.toString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import net.kyori.adventure.text.Component

@Suppress("unused")
@Serializable
enum class ComparisonOperator(val comparator: (String?, String?)->Boolean) {
    @SerialName("=") EQUAL({ f, v -> f == v }),
    @SerialName("!=") NOT_EQUAL({ f, v -> f != v }),
    IN({ f, v -> v?.let { f?.contains(v) ?: false } ?: false }),
    IS_NULL({s, _ -> s == null }),
    IS_NOT_NULL({s, _ -> s != null}),
}

@Suppress("unused")
@Serializable
enum class ListComparisonOperator(val comparator: (String?, List<String?>)->Boolean) {
    IN({f, l -> l.contains(f)}),
    NOT_IN({f, l -> !(l.contains(f))}),
}

@Serializable
enum class LogicalOperator {
    AND,
    OR,
}

@Serializable
enum class ValueProperty {
    WITH_COLOR
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface Filter {
    fun match(fields: Map<String, Component>): Boolean
}

@Serializable
@SerialName("comparison")
data class ComparisonNode(
    val field: String,
    val operator: ComparisonOperator,
    val value: String? = null,
    val properties: List<ValueProperty> = listOf(),
) : Filter {
    override fun match(fields: Map<String, Component>): Boolean {
        val field = fields[field]?.toString(this.properties) ?: return false
        return operator.comparator(field, value)
    }
}

@Serializable
@SerialName("list")
data class ListNode(
    val field: String,
    val operator: ListComparisonOperator,
    val values: List<String?>,
    val properties: List<ValueProperty> = listOf(),
) : Filter {
    override fun match(fields: Map<String, Component>): Boolean {
        val field = fields[field]?.toString(this.properties) ?: return false
        return operator.comparator(field, values)
    }
}

@Serializable
@SerialName("group")
data class GroupNode(
    val operator: LogicalOperator,
    val conditions: List<Filter>,
) : Filter {
    override fun match(fields: Map<String, Component>): Boolean {
        return when(operator) {
            AND -> conditions.all { it.match(fields) }
            OR -> conditions.any { it.match(fields) }
        }
    }
}