package io.kinference.tfjs.data.tensors

import io.kinference.data.ONNXDataType
import io.kinference.data.ONNXTensor
import io.kinference.ndarray.*
import io.kinference.ndarray.arrays.*
import io.kinference.ndarray.arrays.tiled.*
import io.kinference.protobuf.message.TensorProto
import io.kinference.protobuf.message.TensorProto.DataType
import io.kinference.protobuf.resolveProtoDataType
import io.kinference.tfjs.externals.core.NDArrayTFJS
import io.kinference.tfjs.externals.core.tensor
import io.kinference.tfjs.externals.extensions.*
import io.kinference.tfjs.types.ValueInfo
import io.kinference.tfjs.types.ValueTypeInfo
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Int32Array

class TFJSTensor(name: String?, data: NDArrayTFJS, val info: ValueTypeInfo.TensorTypeInfo) : ONNXTensor<NDArrayTFJS>(name, data) {
    constructor(data: NDArrayTFJS, info: ValueInfo) : this(info.name, data, info.typeInfo as ValueTypeInfo.TensorTypeInfo)

    override fun rename(name: String): TFJSTensor {
        return TFJSTensor(name, data, info)
    }

    fun toNDArray(): NDArray {
        val shapeIntArray = data.shape.toIntArray()
        val strides = Strides(shapeIntArray)
        val blockSize = blockSizeByStrides(strides)
        val blocksCount = strides.linearSize / blockSize


        return when(data.dtype) {
            "float32" -> {
                val array = data.dataFloat().unsafeCast<Float32Array>()
                val arrayBuffer = array.buffer
                val blocks = Array(blocksCount) { blockNum ->
                    Float32Array(arrayBuffer, blockNum * blockSize * 4, blockSize).unsafeCast<FloatArray>()
                }
                val tiledArray = FloatTiledArray(blocks)
                FloatNDArray(tiledArray, Strides(shapeIntArray))
            }

            "int32" -> {
                val array = data.dataFloat().unsafeCast<Int32Array>()
                val arrayBuffer = array.buffer
                val blocks = Array(blocksCount) { blockNum ->
                    Int32Array(arrayBuffer, blockNum * blockSize * 4, blockSize).unsafeCast<IntArray>()
                }
                val tiledArray = IntTiledArray(blocks)
                IntNDArray(tiledArray, strides)
            }

            else -> error("Unsupported type")
        }
    }

    companion object {
        //TODO: complex, uint32/64 tensors
        @Suppress("UNCHECKED_CAST")
        fun create(proto: TensorProto): TFJSTensor {
            val type = proto.dataType ?: DataType.UNDEFINED
            val array = parseArray(proto)
            requireNotNull(array) { "Array value should be initialized" }

            return TFJSTensor(array, type, proto.dims, proto.name)
        }

        operator fun invoke(value: NDArray, name: String? = ""): TFJSTensor {
            return when (val resolvedType = value.type.resolveProtoDataType()) {
                DataType.FLOAT -> invoke((value as FloatNDArray).array.toArray(), resolvedType, value.shape, name)
                DataType.INT32 -> invoke((value as IntNDArray).array.toArray(), resolvedType, value.shape, name)
                DataType.UINT8 -> invoke((value as UByteNDArray).array.toArray(), resolvedType, value.shape, name)
                DataType.INT64 -> invoke((value as LongNDArray).array.toArray(), resolvedType, value.shape, name)
                else -> error("Unsupported type")
            }
        }

        private operator fun invoke(value: Any, type: DataType, dims: IntArray, name: String? = ""): TFJSTensor {
            val nameNotNull = name.orEmpty()
            val typedDims = dims.toTypedArray()
            return when (type) {
                DataType.FLOAT -> tensor(value as FloatArray, typedDims, "float32").asTensor(nameNotNull)
                DataType.INT32 -> tensor(value as IntArray, typedDims, "int32").asTensor(nameNotNull)
                DataType.UINT8 -> tensor((value as UByteArray).toTypedArray(), typedDims, "int32").asTensor(nameNotNull)
                DataType.INT8  -> tensor((value as ByteArray).toTypedArray(), typedDims, "int32").asTensor(nameNotNull)
                DataType.INT64 -> tensor((value as LongArray).toIntArray(), typedDims, "int32").asTensor(nameNotNull)
                else -> error("Unsupported type")
            }
        }

        private fun parseArray(proto: TensorProto) = when {
            proto.isPrimitive() -> proto.arrayData
            proto.isString() -> proto.stringData
            else -> error("Unsupported data type ${proto.dataType}")
        }
    }
}