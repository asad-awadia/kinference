package io.kinference.core.operators.layer.recurrent.lstm

import io.kinference.ndarray.arrays.*
import io.kinference.ndarray.extensions.allocateNDArray
import io.kinference.primitives.types.DataType

class LSTMGate private constructor(
    private val weights: AbstractLSTMWeights,
    private val recurrentWeights: AbstractLSTMWeights,
    private val gateData: MutableNumberNDArrayCore,
    private val bias: NumberNDArrayCore?,
    private val peephole: NumberNDArrayCore?
) {

    suspend fun compute(
        input: AbstractLSTMInput,
        lstmStates: LSTMStates,
        activationFunction: PrimitiveToPrimitiveFunction,
        numDirection: Int,
        batchNum: Int
    ) {
        val gateLocal = gateData.viewMutable(batchNum)
        gateLocal.clean()

        input.dot(weights, gateLocal)
        lstmStates.hiddenState.getVector(numDirection, batchNum).dot(recurrentWeights, gateLocal)
        if (bias != null) gateLocal.plusAssign(bias)
        if (peephole != null) gateLocal.plusAssign(peephole.times(lstmStates.cellState.getVector(numDirection, batchNum)))
        gateLocal.mapMutable(activationFunction)
    }

    fun getVector(batchNum: Int) = gateData.view(batchNum)

    companion object {
        internal suspend operator fun invoke(
            weights: AbstractLSTMWeights,
            recurrentWeights: AbstractLSTMWeights,
            bias: NumberNDArrayCore?,
            peephole: NumberNDArrayCore?,
            batchSize: Int, hiddenSize: Int, dataType: DataType
        ): LSTMGate {

            val gateData = allocateNDArray(dataType, intArrayOf(batchSize, hiddenSize)) as MutableNumberNDArrayCore
            val gate = LSTMGate(weights, recurrentWeights, gateData, bias, peephole)

            return gate
        }
    }
}

data class LSTMGates(val input: LSTMGate, val output: LSTMGate, val forget: LSTMGate, val cell: LSTMGate) {
    companion object {
        suspend fun create(weights: AbstractLSTMWeights, recurrentWeights: AbstractLSTMWeights, bias: NumberNDArrayCore?,
                           peepholes: NumberNDArrayCore?, batchSize: Int, hiddenSize: Int, dataType: DataType): LSTMGates {
            val inputGate = LSTMGate(
                weights.view(0),
                recurrentWeights.view(0),
                bias?.view(0)?.plus(bias.view(4)),
                peepholes?.view(0),
                batchSize, hiddenSize, dataType
            )
            val outputGate = LSTMGate(
                weights.view(1),
                recurrentWeights.view(1),
                bias?.view(1)?.plus(bias.view(5)),
                peepholes?.view(1), batchSize, hiddenSize, dataType
            )
            val forgetGate = LSTMGate(
                weights.view(2),
                recurrentWeights.view(2),
                bias?.view(2)?.plus(bias.view(6)),
                peepholes?.view(2), batchSize, hiddenSize, dataType
            )
            val cellGate = LSTMGate(
                weights.view(3),
                recurrentWeights.view(3),
                bias?.view(3)?.plus(bias.view(7)),
                null,
                batchSize, hiddenSize, dataType
            )

            return LSTMGates(inputGate, outputGate, forgetGate, cellGate)
        }
    }
}

class LSTMCellState private constructor(
    private val stateData: MutableNumberNDArrayCore,
    private val tempData: MutableNumberNDArrayCore
) {

    val data: NumberNDArrayCore
        get() = stateData

    suspend fun compute(lstmGates: LSTMGates, numDirection: Int, batchNum: Int) {
        val stateLocal = stateData.viewMutable(numDirection, batchNum)
        val tempLocal = tempData.viewMutable(numDirection, batchNum)

        stateLocal.timesAssign(lstmGates.forget.getVector(batchNum))
        lstmGates.input.getVector(batchNum).times(lstmGates.cell.getVector(batchNum), tempLocal)
        stateLocal.plusAssign(tempLocal)
    }

    fun getVector(numDirection: Int, batchNum: Int) = stateData.view(numDirection, batchNum)

    companion object {
        internal suspend operator fun invoke(
            initCellState: NumberNDArrayCore?, dataType: DataType,
            numDirections: Int, batchSize: Int, hiddenSize: Int
        ): LSTMCellState {

            val stateData = initCellState?.toMutable() ?: allocateNDArray(dataType, intArrayOf(numDirections, batchSize, hiddenSize)) as MutableNumberNDArrayCore
            val tempData = allocateNDArray(dataType, intArrayOf(numDirections, batchSize, hiddenSize)) as MutableNumberNDArrayCore
            val cellState = LSTMCellState(stateData, tempData)

            return cellState
        }
    }
}

class LSTMHiddenState internal constructor(
    initHiddenState: MutableNumberNDArrayCore,
    initHiddenStateAsLSTMInput: Array<AbstractLSTMInput>,
    private val activationFunctions: List<PrimitiveToPrimitiveFunction>
) {
    private val stateData = initHiddenState
    val data: NumberNDArrayCore
        get() = stateData

    private val stateDataAsLSTMInput = initHiddenStateAsLSTMInput

    suspend fun compute(lstmGates: LSTMGates, cellState: LSTMCellState, numDirection: Int, batchNum: Int) {
        val stateLocal = stateData.viewMutable(numDirection, batchNum)
        stateLocal.copyFrom(0, cellState.getVector(numDirection, batchNum))
        stateLocal.mapMutable(activationFunctions[numDirection])
        stateLocal.timesAssign(lstmGates.output.getVector(batchNum))
    }

    suspend fun update(numDirection: Int) {
        stateDataAsLSTMInput[numDirection] = stateDataAsLSTMInput[numDirection].recreate(stateData.view(numDirection))
    }

    fun getVector(numDirection: Int, batchNum: Int): AbstractLSTMInput = stateDataAsLSTMInput[numDirection].view(batchNum)

    fun getVectorRaw(numDirection: Int, batchNum: Int): NumberNDArray = data.view(numDirection, batchNum)

}

data class LSTMStates(val cellState: LSTMCellState, val hiddenState: LSTMHiddenState)

data class LSTMLayerOutput(
    val output: NumberNDArrayCore,
    val hiddenState: NumberNDArrayCore,
    val cellState: NumberNDArrayCore
)
