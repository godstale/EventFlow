package com.example.eventflow

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hardcopy.eventflow.EventFlow
import com.hardcopy.eventflow.eventbus.EventFlowControl
import com.hardcopy.eventflow.utils.EventFlowDebugUtil
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.tvLog)

        Log.d("###", "EventFlow initializing....")
        EventFlow.initialize()
        EventFlowDebugUtil.setDebugMode(true)
        EventFlowDebugUtil.useTopicDump(true)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()

        Log.d("###", "=====[Simple test]=====================================")
        simpleTest()

//        Log.d("###", "=====[Basic test]======================================")
//        basicTest()

//        Log.d("###", "=====[Publish test]======================================")
//        publishTest()

//        Log.d("###", "=====[Remove topic test]======================================")
//        removeTopicTest()

//        Log.d("###", "=====[EventFlow builder test]======================================")
//        builderTest()

//        Log.d("###", "=====[Type casting error test]======================================")
//        typeCastingErrorTest()

//        Log.d("###", "=====[Flow control test]======================================")
//        flowControlTest()
    }

    private fun simpleTest() {
        // use default topic if topic param is empty
        EventFlow.subscribe<String>(lifecycleScope, {
            showLog("  -->[/sys/common] event received = $it")
        }, {
            showLog("  -->[/sys/common] exception = ${it.localizedMessage}")
        })

        showLog("[/sys/common] Publish message")
        EventFlow.publish("Hello world!!")
    }

    private fun basicTest() {
        val TOPIC_TEST_BASIC = "/test/basic"

        // create a topic
        EventFlow.builder()
            .setTopic(TOPIC_TEST_BASIC)
            .build()

        // subscribe a topic twice
        val subscriberJob1 = EventFlow.subscribe<String>(TOPIC_TEST_BASIC, lifecycleScope) {
            showLog("  -->[$TOPIC_TEST_BASIC, #1] $it")
        }
        val subscriberJob2 = EventFlow.subscribe<String>(TOPIC_TEST_BASIC, lifecycleScope) {
            showLog("  -->[$TOPIC_TEST_BASIC, #2] $it")
        }

        lifecycleScope.launch {
            delay(100)

            // publish data to a specified topic
            showLog("[$TOPIC_TEST_BASIC] Publish message")
            EventFlow.publish(TOPIC_TEST_BASIC, "Hello world!!", false)

            delay(100)

            // unsubscribe topic
            subscriberJob1?.cancel()

            // publish data again
            showLog("[$TOPIC_TEST_BASIC] Publish message2")
            EventFlow.publish(TOPIC_TEST_BASIC, "Hello world!! - 2", false)
        }
    }

    private fun publishTest() {
        val TOPIC_TEST_PUBLISH = "/test/pub"
        val TOPIC_TEST_PUBLISH1 = "/test/pub/depth1"
        val TOPIC_TEST_PUBLISH2A = "/test/pub/depth1/depth2a"
        val TOPIC_TEST_PUBLISH2B = "/test/pub/depth1/depth2b"

        // subscribe() makes topic [/test/pub] if it doesn't exist
        EventFlow.subscribe<String>(TOPIC_TEST_PUBLISH, lifecycleScope) {
            showLog("  -->[$TOPIC_TEST_PUBLISH, #1] $it")
        }

        // create topic [/test/pub/depth1] and subscribe
        EventFlow.subscribe<String>(TOPIC_TEST_PUBLISH1, lifecycleScope) {
            showLog("  -->[$TOPIC_TEST_PUBLISH1] $it")
        }

        // create topic [/test/pub/depth1/depth2a] and observe
        EventFlow.subscribe<String>(TOPIC_TEST_PUBLISH2A, lifecycleScope) {
            showLog("  -->[$TOPIC_TEST_PUBLISH2A] $it")
        }

        // create topic [/test/pub/depth1/depth2b] and observe
        EventFlow.subscribe<String>(TOPIC_TEST_PUBLISH2B, lifecycleScope) {
            showLog("  -->[$TOPIC_TEST_PUBLISH2B] $it")
        }

        lifecycleScope.launch {
            delay(100)

            showLog("[$TOPIC_TEST_PUBLISH] Publish message (to single topic)")
            EventFlow.publish(TOPIC_TEST_PUBLISH, "Hello world!! 1", false)

            delay(100)

            showLog(" ")
            showLog("[$TOPIC_TEST_PUBLISH] Publish message (recursive)")
            EventFlow.publish(TOPIC_TEST_PUBLISH, "Hello world!! 2")
        }
    }

    private fun removeTopicTest() {
        val TOPIC_TEST_REMOVE = "/test/pub"
        val TOPIC_TEST_REMOVE1 = "/test/pub/depth1"
        val TOPIC_TEST_REMOVE2A = "/test/pub/depth1/depth2a"
        val TOPIC_TEST_REMOVE2B = "/test/pub/depth1/depth2b"

        // create a topic [/test/pub] and subscribe
        EventFlow.subscribe<String>(TOPIC_TEST_REMOVE, lifecycleScope) {
            showLog("  -->[$TOPIC_TEST_REMOVE] $it")
        }

        // create topic [/test/pub/depth1] and subscribe
        EventFlow.subscribe<String>(TOPIC_TEST_REMOVE1, lifecycleScope) {
            showLog("  -->[$TOPIC_TEST_REMOVE1] $it")
        }

        // create topic [/test/pub/depth1/depth2a] and subscribe
        EventFlow.subscribe<String>(TOPIC_TEST_REMOVE2A, lifecycleScope) {
            showLog("  -->[$TOPIC_TEST_REMOVE2A] $it")
        }

        // create topic [/test/pub/depth1/depth2b] and subscribe
        EventFlow.subscribe<String>(TOPIC_TEST_REMOVE2B, lifecycleScope) {
            showLog("  -->[$TOPIC_TEST_REMOVE2B] $it")
        }

        lifecycleScope.launch {
            delay(100)

            // Publish a message
            showLog("[$TOPIC_TEST_REMOVE] Publish message (Recursive)")
            EventFlow.publish(TOPIC_TEST_REMOVE, "Hello world!! - 1")

            delay(100)

            // Remove a topic
            showLog("")
            showLog("[$TOPIC_TEST_REMOVE2B] Remove a topic")
            EventFlow.remove(TOPIC_TEST_REMOVE2B)

            delay(100)

            // Publish a message
            showLog("")
            showLog("[$TOPIC_TEST_REMOVE] Publish message (Recursive)")
            EventFlow.publish(TOPIC_TEST_REMOVE, "Hello world!! - 2")

            delay(100)

            // Remove top level topic and descendants
            showLog("")
            showLog("[$TOPIC_TEST_REMOVE] Remove topics")
            EventFlow.remove(TOPIC_TEST_REMOVE)

            delay(100)

            // Publish a message but there's no subscriber.
            showLog("[$TOPIC_TEST_REMOVE] Publish message (Recursive)")
            EventFlow.publish(TOPIC_TEST_REMOVE, "Hello world!! - 3")
        }
    }

    private fun builderTest() {
        // create a topic using canonical name of class
        EventFlow.builder()
            .setTopic(MainActivity::class.java)
            .setBufferSize(32)
            .withBackpressure(EventFlowControl.BpType.DROP_OLDEST)
            .withValve()
            .build()

        // subscribe a topic twice
        EventFlow.subscribe<String>(MainActivity::class.java, lifecycleScope) {
            showLog("  -->[Subscriber #1] $it")
        }
        EventFlow.subscribe<String>(MainActivity::class.java, lifecycleScope) {
            showLog("  -->[Subscriber #2] $it")
        }

        lifecycleScope.launch {
            delay(100)

            // Publish a message
            showLog("[MainActivity] Publish a message")
            EventFlow.publish(MainActivity::class.java, "Hello world!! - 1")

            // Switch valve
            delay(100)
            showLog("")
            showLog("  -->[Topic : MainActivity] close valve")
            EventFlow.switchTopicValve(MainActivity::class.java, open = false)

            // Publish a message
            delay(100)
            showLog("")
            showLog("[MainActivity] Publish a message")
            EventFlow.publish(MainActivity::class.java, "Hello world!! - 2")

            // Switch valve
            delay(100)
            showLog("")
            showLog("  -->[Topic : MainActivity] open valve")
            EventFlow.switchTopicValve(MainActivity::class.java, open = true)

            // Publish a message
            delay(100)
            showLog("")
            showLog("[MainActivity] Publish a message")
            EventFlow.publish(MainActivity::class.java, "Hello world!! - 3")
        }
    }

    private fun typeCastingErrorTest() {
        // subscribe a topic
        EventFlow.subscribe<Int>(MainActivity::class.java,
            lifecycleScope,
            // Set callback
            {
                showLog("  -->[Subscriber #1] integer = $it")
            },
            // Set error handler
            {
                showLog("")
                it.localizedMessage?.let { it1 -> showLog(it1) }
            })

        lifecycleScope.launch {
            delay(100)

            // Publish a message. This event makes below exception.
            // java.lang.String cannot be cast to java.lang.Number
            showLog("[MainActivity] Publish a message")
            EventFlow.publish(MainActivity::class.java, "Hello world!!")
        }
    }

    private fun showLog(text: String) {
        textView.append("$text \n")
    }
}