package com.hardcopy.eventflow

import com.hardcopy.eventflow.eventbus.EventBus
import com.hardcopy.eventflow.eventbus.EventProcessor
import com.hardcopy.eventflow.eventbus.EventTopic
import com.hardcopy.eventflow.exceptions.EventFlowException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * EventFlow API class.
 * Every event-topic stream starts from this APIs.
 *
 * Created by godstale@hotmail.com(Young-bae Suh)
 */
object EventFlow {
    private lateinit var eventBus: EventBus

    /**
     * Initialize EventFlow core.
     * WARNING: This call must be the first call of EventFlow APIs.
     */
    fun initialize() {
        if(!::eventBus.isInitialized) {
            eventBus = EventBus()
        }
    }

    /**
     * Get an EventFlowBuilder to start topic stream.
     *
     * @return  EventFlowBuilder
     */
    fun builder(): EventFlowBuilder {
        if(!::eventBus.isInitialized) throw EventFlowException.NOT_INITIALIZED.exception
        return EventFlowBuilder.create(eventBus)
    }

    /**
     * Publish new event to system-default topic. (/sys/common)
     *
     * @param   contents
     * @return  Boolean
     */
    fun publish(contents: Any): Boolean {
        if(!::eventBus.isInitialized) throw EventFlowException.NOT_INITIALIZED.exception

        // use system topic [/sys/common], so no need to use recursive
        eventBus.publishEvent(EventTopic.TOPIC_COMMON, contents, false)
        return true
    }

    /**
     * Publish an event to specific topic.
     * "recursive = true" means deliver event to target topic and descendants.
     *
     * @param   topic           topic path
     * @param   contents
     * @param   recursive       deliver event to descendants also
     * @return  Boolean
     */
    fun publish(topic: String, contents: Any, recursive: Boolean = true): Boolean {
        if(!::eventBus.isInitialized) throw EventFlowException.NOT_INITIALIZED.exception
        if(!EventTopic.isValidForPublish(topic)) return false

        eventBus.publishEvent(topic, contents, recursive)
        return true
    }

    /**
     * Publish an event with class type.
     * EventFlow converts class type to topic string using (/sys/class) topic path.
     * ex> com.exam.event.MainActivity -> /sys/class/com.exam.event.MainActivity
     *
     * @param   clazz           class type
     * @param   contents
     * @param   recursive       deliver event to descendants also
     * @return  Boolean
     */
    fun publish(clazz: Class<*>, contents: Any, recursive: Boolean = true): Boolean {
        if(!::eventBus.isInitialized) throw EventFlowException.NOT_INITIALIZED.exception

        eventBus.publishEvent(EventTopic.getClassTopicString(clazz), contents, recursive)
        return true
    }

    /**
     * Remove topic and descendants. (Always removes descendants also.)
     *
     * @param   topic
     */
    fun remove(topic: String) {
        if(!::eventBus.isInitialized) throw EventFlowException.NOT_INITIALIZED.exception
        if(!EventTopic.isValidForRemove(topic)) return

        eventBus.removeEventProcessors(topic)
    }

    /**
     * Remove topic and descendants. (Always removes descendants also.)
     *
     * @param   clazz           class type
     */
    fun remove(clazz: Class<*>) {
        if(!::eventBus.isInitialized) throw EventFlowException.NOT_INITIALIZED.exception
        remove(EventTopic.getClassTopicString(clazz))
    }

    /**
     * Observes default(/sys/common) topic with onNext lamda function.
     *
     * @param   scope       coroutine scope to run SharedFlow.collect {}
     * @param   onNext      lamda function to receive event
     * @return  CoroutineContext?     WARNING: Be sure to release resources with CoroutineContext after use
     */
    fun <T> subscribe(scope: CoroutineScope,
                      onNext: (T) -> Unit
    ): Job? {
        return subscribe(EventTopic.TOPIC_COMMON, scope, onNext, {})
    }

    /**
     * Observes default(/sys/common) topic with onNext, onError lamda function.
     *
     * @param   scope       coroutine scope to run SharedFlow.collect {}
     * @param   onNext      lamda function to receive event
     * @param   onError     lamda function to receive error event
     * @return  CoroutineContext?     WARNING: release resources with CoroutineContext after use
     */
    fun <T> subscribe(scope: CoroutineScope,
                      onNext: (T) -> Unit,
                      onError: (t: Throwable) -> Unit = {}
    ): Job? {
        return subscribe(EventTopic.TOPIC_COMMON, scope, onNext, onError)
    }

    /**
     * Observes class(/sys/class/xxx) topic with class type and onNext lamda function.
     *
     * @param   clazz       class type
     * @param   scope       coroutine scope to run SharedFlow.collect {}
     * @param   onNext      lamda function to receive event
     * @return  CoroutineContext?     WARNING: release resources with CoroutineContext after use
     */
    fun <T> subscribe(clazz: Class<*>,
                      scope: CoroutineScope,
                      onNext: (T) -> Unit
    ): Job? {
        return subscribe(EventTopic.getClassTopicString(clazz), scope, onNext)
    }

    /**
     * Observes class(/sys/class/xxx) topic with class type and onNext, onError lamda function.
     *
     * @param   clazz       class type
     * @param   scope       coroutine scope to run SharedFlow.collect {}
     * @param   onNext      lamda function to receive event
     * @param   onError     lamda function to receive error event
     * @return  CoroutineContext?     WARNING: release resources with CoroutineContext after use
     */
    fun <T> subscribe(clazz: Class<*>,
                      scope: CoroutineScope,
                      onNext: (T) -> Unit,
                      onError: (t: Throwable) -> Unit = {}
    ): Job? {
        return subscribe(EventTopic.getClassTopicString(clazz), scope, onNext, onError)
    }

    /**
     * Observes topic with onNext lamda.
     *
     * @param   topic       topic path
     * @param   scope       coroutine scope to run SharedFlow.collect {}
     * @param   onNext      lamda function to receive event
     * @return  CoroutineContext?     WARNING: release resources with CoroutineContext after use
     */
    fun <T> subscribe(topic: String,
                      scope: CoroutineScope,
                      onNext: (T) -> Unit
    ): Job? {
        return subscribe(topic, scope, onNext, {})
    }

    /**
     * Observes topic with lamda/composite disposable.
     *
     * @param   topic       topic path
     * @param   scope       coroutine scope to run SharedFlow.collect {}
     * @param   onNext      lamda function to receive event
     * @param   onError     lamda function to receive error event
     * @return  CoroutineContext?     WARNING: release resources with CoroutineContext after use
     */
    fun <T> subscribe(topic: String,
                      scope: CoroutineScope,
                      onNext: (T) -> Unit,
                      onError: (t: Throwable) -> Unit
    ): Job? {
        if(!::eventBus.isInitialized) throw EventFlowException.NOT_INITIALIZED.exception
        if(!EventTopic.isValidForSubscribe(topic)) return null

        val eventProcessor = getTopicOrCreate(topic)
        var job: Job? = null

        eventProcessor?.also { processor ->
            job = scope.launch(SupervisorJob()) {
                runCatching {
                    processor.getSharedFlow()?.collect { event ->
                        onNext(event as T)
                    }
                }.onSuccess {
                }.onFailure {
                    onError(it)
                }
            }
        }

        return job
    }

    /**
     * Close or open the valve on topic stream.
     *
     * @param   clazz       class type
     * @param   open        open or close
     * @return  Boolean     success/fail
     */
    fun switchTopicValve(clazz: Class<*>, open: Boolean): Boolean {
        return switchTopicValve(EventTopic.getClassTopicString(clazz), open)
    }

    /**
     * Close or open the valve on topic stream.
     *
     * @param   topic       topic string
     * @param   open        open or close
     * @return  Boolean     success/fail
     */
    fun switchTopicValve(topic: String, open: Boolean): Boolean {
        if(!::eventBus.isInitialized) throw EventFlowException.NOT_INITIALIZED.exception
        if(!EventTopic.isValidForSubscribe(topic)) return false

        val eventProcessor = eventBus.getProcessor(topic) ?: return false
        return eventProcessor.switchValve(open)
    }

    /**
     * Create topic if not exist and get EventProcessor instance to control topic.
     *
     * @param   topic       topic string
     * @return  EventProcessor      EventProcessor instance to control topic
     */
    private fun getTopicOrCreate(topic: String): EventProcessor? {
        createTopicIfNotExist(topic)
        return eventBus.getProcessor(topic)
    }

    /**
     * Create topic if not exist.
     *
     * @param   topic       topic string
     * @return  EventProcessor      EventProcessor instance to control topic
     */
    private fun createTopicIfNotExist(topic: String) {
        eventBus.getProcessor(topic)?: run {
            builder().setTopic(topic).build()
        }
    }
}