### EventFlow: Topic based event bus for Android [![](https://jitpack.io/v/godstale/EventFlow.svg)](https://jitpack.io/#godstale/EventFlow)

EventFlow provides coroutine based event bus, especially focused on effective management of multiple event streams. To achieve this EventFlow implements MQTT style topic management.

EventFlow uses Kotlin, Coroutine and SharedFlow for the event bus core. Check another event bus library using reactive stream. [Godstale's EventPress](https://github.com/godstale/EventPress "ReactiveX style event bus").

### Feature list

* Manage multiple event streams with topic hierarchy like MQTT protocol does.
* Each event stream you make has it's own topic path like "/viewmodel/logic/stream1"
* Supports "make", "observe" or "publish" actions on a topic and it affects target topic and descendants.
* EventFlow provides APIs for the backpressure strategy and valve control.  
* it's very **lightweight**
 
### Gradle settings (via [JitPack.io](https://jitpack.io/))

1. In your project level **build.gradle**:
```groovy
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```
2. Add the dependency at module level `build.gradle`: curret latest version is **1.0.1**
```groovy
dependencies {
	implementation 'com.github.godstale:EventFlow:<LATEST-VERSION>'
}
```

### How to use

*Content*

- [Topic](#topic)
- [Test code](#test-code)
- [Simple usage](#simple-usage)
- [Basic usage](#basic-usage)
- [Publish an event](#publish-an-event)
- [Remove topics](#remove-topics)
- [Builder test](#builder-test)
- [Error control](#error-control)
- [Finally](#finally)

-----------------------------------------------------------------------------------------------------------------------

##### Topic

Topic is the hierarchical expression of each event stream which uses '/' as divider like path of file system. For example:

* /api
* /api/member
* /api/member/login
* /api/member/info

If you made topic hierarchy like example, publishing an event on the topic */api/member* delivers event to */api/member*, */api/member/logic* and */api/member/info* also.

You can name each event stream as you wish. But you have to follow below rules.

* Topic string must starts with '/'
* Topic string should not end with '/'
* Do not use root '/' expression at API call
* No blank is allowed
* Only [ . _ 0-9 a-z A-Z / - ] is allowed
* Max length is 256
* each topic name has one or more characters.
* /sys, /sys/class, /sys/ui and /sys/common is reserved. Do not use these topics on API call.
* Doesn't support wild card character.
-----------------------------------------------------------------------------------------------------------------------

##### Test code

Check out the [MainActivity](https://github.com/godstale/EventFlow/blob/main/app/src/main/java/com/example/eventflow/MainActivity.kt), there are test codes to check out basic usage of EventFlow.

-----------------------------------------------------------------------------------------------------------------------

##### Simple usage

Before calling the EventFlow APIs, initialize EventFlow first. (Application class is good to do this) :

    EventFlow.initialize()

Most simpe way to observe and publish events. :

	EventFlow.subscribe<String>(lifecycleScope, {
		showLog("  -->[/sys/common] event received = $it")
	}, {
		showLog("  -->[/sys/common] exception = ${it.localizedMessage}")
	})

	showLog("[/sys/common] Publish message")
	EventFlow.publish("Hello world!!")

No topic definition is found in this example but EventFlow core uses common topic, **/sys/common** as default.

Very easy to use but keep in mind that all the observer on this topic uses same object type over the application. (See the type casting of **String**)

-----------------------------------------------------------------------------------------------------------------------

##### Basic usage

Observe and publish events on custom topic. :

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
		EventFlow.publish(TOPIC_TEST_BASIC, "Hello world!!", false)
		delay(100)

		// unsubscribe topic
		subscriberJob1?.cancel()

		// publish data again
		EventFlow.publish(TOPIC_TEST_BASIC, "Hello world!! - 2", false)
	}


You can make a topic, event stream, with EventFlowBuilder. But without builder code, you can receive events by calling **EventFlow.subscribe<>()**. Because EventFlow makes topic automatically if it's not exist.    

**EventFlow.subscribe<>()** attach your lambda function to **Coroutine SharedFlow** event stream.(internally calls SharedFlow.collect(your_lambda()))
And subscribe() function returns **coroutine Job instance**. You can cancel subscription and release resources with Job.cancel().

**Keep in mind** that you have to release resources after use. **Job.cancel()** removes single subscription. **EventFlow.remove(topic)** closes topic and all subscribers.

-----------------------------------------------------------------------------------------------------------------------

##### Publish an event

Publish an event to topics. :

	val TOPIC_TEST_PUBLISH = "/test/pub"
	val TOPIC_TEST_PUBLISH1 = "/test/pub/depth1"
	val TOPIC_TEST_PUBLISH2A = "/test/pub/depth1/depth2a"
	val TOPIC_TEST_PUBLISH2B = "/test/pub/depth1/depth2b"

	// subscribe() makes a topic [/test/pub] if it doesn't exist
	EventFlow.subscribe<String>(TOPIC_TEST_PUBLISH, lifecycleScope) {
		showLog("  -->[$TOPIC_TEST_PUBLISH, #1] $it")
	}

	// create a topic [/test/pub/depth1] and subscribe
	EventFlow.subscribe<String>(TOPIC_TEST_PUBLISH1, lifecycleScope) {
		showLog("  -->[$TOPIC_TEST_PUBLISH1] $it")
	}

	// create a topic [/test/pub/depth1/depth2a] and subscribe
	EventFlow.subscribe<String>(TOPIC_TEST_PUBLISH2A, lifecycleScope) {
		showLog("  -->[$TOPIC_TEST_PUBLISH2A] $it")
	}

	// create a topic [/test/pub/depth1/depth2b] and subscribe
	EventFlow.subscribe<String>(TOPIC_TEST_PUBLISH2B, lifecycleScope) {
		showLog("  -->[$TOPIC_TEST_PUBLISH2B] $it")
	}

	lifecycleScope.launch {
		delay(100)

		showLog("[$TOPIC_TEST_PUBLISH] Publish message (to single topic)")
		EventFlow.publish(TOPIC_TEST_PUBLISH, "Hello world!! 1", false)

		delay(100)

		showLog(" ")
		showLog("[$TOPIC_TEST_PUBLISH] Publish message (recursive to descendants)")
		EventFlow.publish(TOPIC_TEST_PUBLISH, "Hello world!! 2")
	}

**recursive = false** parameter targets only one topic.

-----------------------------------------------------------------------------------------------------------------------

##### Remove topics

Test to remove a topic and descendants :

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
		showLog("[$TOPIC_TEST_REMOVE2B] Remove a topic")
		EventFlow.remove(TOPIC_TEST_REMOVE2B)
		delay(100)

		// Publish a message
		showLog("[$TOPIC_TEST_REMOVE] Publish message (Recursive)")
		EventFlow.publish(TOPIC_TEST_REMOVE, "Hello world!! - 2")
		delay(100)

		// Remove top level topic and descendants
		showLog("[$TOPIC_TEST_REMOVE] Remove topics")
		EventFlow.remove(TOPIC_TEST_REMOVE)
		delay(100)

		// Publish a message but there's no subscriber.
		showLog("[$TOPIC_TEST_REMOVE] Publish message (Recursive)")
		EventFlow.publish(TOPIC_TEST_REMOVE, "Hello world!! - 3")
	}

Always EventFlow.remove() deletes target topic and descendants.

-----------------------------------------------------------------------------------------------------------------------

##### Builder test

How to use builder to apply options to the topic :

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

Add **.withValve()** in builder method chain and call **EventFlow.switchTopicValve()** to switch on and off the stream. (It affects every topic subscriber)

Set buffer size with **.setBufferSize()** method. Default is 64.

Add back pressure policy with **.withBackpressure()**. You can select one of three types - EventFlowControl.BpType.SUSPEND, EventFlowControl.BpType.DROP_OLDEST and EventFlowControl.BpType.DROP_LATEST. DROP_OLDEST is default.

-----------------------------------------------------------------------------------------------------------------------

##### Error control

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

In this example I subscribed topic with Int type but published an event with String object. To catch unexpected errors like this example, add error handler when you subscribe a topic.

-----------------------------------------------------------------------------------------------------------------------

##### Finally

**EventFlow.subscribe<>()** returns **coroutine Job** instance.

**Do not forget calling Job.cancel() after use.** Subscribers keep alive until user removes the topic. Especially subscribers on system reserved topic(/sys/xxx) lasts until user terminates the app if you don't dispose it.

.

.
