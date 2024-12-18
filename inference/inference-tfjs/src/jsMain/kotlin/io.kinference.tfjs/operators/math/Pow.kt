package io.kinference.tfjs.operators.math

import io.kinference.attribute.Attribute
import io.kinference.data.ONNXData
import io.kinference.graph.Contexts
import io.kinference.ndarray.arrays.NumberNDArrayTFJS
import io.kinference.ndarray.extensions.pow
import io.kinference.operator.*
import io.kinference.protobuf.message.TensorProto
import io.kinference.tfjs.data.tensors.TFJSTensor
import io.kinference.tfjs.data.tensors.asTensor

sealed class Pow(
    name: String,
    info: OperatorInfo,
    attributes: Map<String, Attribute<Any>>,
    inputs: List<String>,
    outputs: List<String>
) : Operator<TFJSTensor, TFJSTensor>(name, info, attributes, inputs, outputs) {
    companion object {
        private val DEFAULT_VERSION = VersionInfo(sinceVersion = 7)

        operator fun invoke(name: String, version: Int?, attributes: Map<String, Attribute<Any>>, inputs: List<String>, outputs: List<String>): Pow {
            return when (version ?: DEFAULT_VERSION.sinceVersion) {
                in PowVer7.VERSION.asRange() -> PowVer7(name, attributes, inputs, outputs)
                else -> error("Unsupported version of Pow operator: $version")
            }
        }
    }
}


class PowVer7(
    name: String,
    attributes: Map<String, Attribute<Any>>,
    inputs: List<String>,
    outputs: List<String>
) : Pow(name, INFO, attributes, inputs, outputs) {
    companion object {
        private val INPUT_TYPE_CONSTRAINTS = FLOAT_DATA_TYPES + setOf(TensorProto.DataType.INT32, TensorProto.DataType.INT64)
        private val POW_TYPE_CONSTRAINTS = FLOAT_DATA_TYPES + INT_DATA_TYPES + UINT_DATA_TYPES

        private val INPUTS_INFO = listOf(
            IOInfo(0, INPUT_TYPE_CONSTRAINTS, "X", optional = false),
            IOInfo(1, POW_TYPE_CONSTRAINTS, "Y", optional = false)
        )

        private val OUTPUTS_INFO = listOf(
            IOInfo(0, INPUT_TYPE_CONSTRAINTS, "Z", optional = false)
        )

        internal val VERSION = VersionInfo(sinceVersion = 7)
        private val INFO = OperatorInfo("Pow", emptyMap(), INPUTS_INFO, OUTPUTS_INFO, VERSION, OperatorInfo.DEFAULT_DOMAIN)
    }

    override suspend fun <D : ONNXData<*, *>> apply(contexts: Contexts<D>, inputs: List<TFJSTensor?>): List<TFJSTensor?> {
        val input = inputs[0]!!.data as NumberNDArrayTFJS
        val powArray = inputs[1]!!.data as NumberNDArrayTFJS
        return listOf(input.pow(powArray).asTensor("Z"))
    }
}
