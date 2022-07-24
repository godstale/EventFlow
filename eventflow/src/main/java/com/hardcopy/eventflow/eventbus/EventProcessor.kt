package com.hardcopy.eventflow.eventbus

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * EventProcess holds every settings and instances related to the topic.
 * EventFlowBuilder make and initializes this instance and
 * EventBus makes main worker, SharedFlow, at registration time.
 *
 * Created by godstale@hotmail.com(Young-bae Suh)
 */
class EventProcessor(val topic: EventTopic,
                     private val flowControl: EventFlowControl
) {
    private val TAG = "EventProcessor"
    private var processor: MutableSharedFlow<Any>? = null
    private val processorJob = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        Log.d(TAG, "Exception = ${throwable.localizedMessage}\n" +
                "Coroutine context = ${coroutineContext.hashCode()}\n" +
                "${throwable.printStackTrace()}\n")
    }
    private val eventScope: CoroutineScope = CoroutineScope(
        Dispatchers.Default + processorJob + exceptionHandler
    )
    private var collectCount = 0

    /**
     * Set SharedFlow instance.
     * FlowableProcessor is interface name of PublishProcessor
     *
     * @param   sf          MutableSharedFlow<Any>
     */
    fun setSharedFlow(sf: MutableSharedFlow<Any>) {
        processor = sf
        eventScope.launch {
            processor?.subscriptionCount?.collect {
                collectCount = it
            }
        }
    }

    /**
     * Return SharedFlow instance.
     *
     * @return   SharedFlow<Any>
     */
    fun getSharedFlow(): SharedFlow<Any>? {
        return processor
    }

    /**
     * Publish event (Check valve status)
     *
     * @param   content
     */
    fun publish(content: Any) {
        if(!flowControl.isValveEnabled() || flowControl.isValveOpened) {
            eventScope.launch {
                processor?.emit(content)
            }
        }
    }

    /**
     * Close or open the valve if valve is available
     *
     * @param   isOpened
     */
    fun switchValve(open: Boolean): Boolean {
        return if(flowControl.isValveEnabled()) {
            flowControl.switchValve(open)
        } else false
    }

    /**
     * Get buffer size
     *
     * @return   Int        buffer size
     */
    fun getBufferSize(): Int {
        return flowControl.getBufferSize()
    }

    /**
     * Dispose all the disposals on this processor
     */
    fun hasSubscribers(): Boolean {
        return collectCount > 0
    }

    /**
     * Dispose all the disposals on this processor
     *
     * @return      isOpened
     */
    fun countSubscribers(): Int {
        return collectCount
    }

    /**
     * Stop PublishProcessor stream and release resources.
     */
    fun stopProcessor() {
        // Stop all coroutines in this scope.
        // From now on, scope is not available.
        eventScope.cancel()

        // Stops child coroutines but eventScope is still available.
        // eventScope.coroutineContext.cancelChildren()
    }
}