package io.kinference.tfjs.operators.layer.recurrent.lstm

import io.kinference.tfjs.runners.TFJSTestEngine.TFJSAccuracyRunner
import io.kinference.utils.TestRunner
import kotlin.test.Test

class LSTMLayerTest {
    private fun getTargetPath(dirName: String) = "lstm/$dirName/"

    @Test
    fun test_LSTM_defaults() = TestRunner.runTest {
        TFJSAccuracyRunner.runFromResources(getTargetPath("test_lstm_defaults"))
    }

    @Test
    fun test_LSTM_with_initial_bias() = TestRunner.runTest {
        TFJSAccuracyRunner.runFromResources(getTargetPath("test_lstm_with_initial_bias"))
    }

    @Test
    fun test_LSTM_with_peepholes() = TestRunner.runTest {
        TFJSAccuracyRunner.runFromResources(getTargetPath("test_lstm_with_peepholes"))
    }

    @Test
    fun test_BiLSTM_defaults() = TestRunner.runTest {
        TFJSAccuracyRunner.runFromResources(getTargetPath("test_bilstm_defaults"))
    }

    @Test
    fun test_BiLSTM_with_bias() = TestRunner.runTest {
        TFJSAccuracyRunner.runFromResources(getTargetPath("test_bilstm_with_bias"))
    }
}