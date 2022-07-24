package com.hardcopy.eventflow.eventbus

import android.util.Log
import com.hardcopy.eventflow.utils.EventFlowDebugUtil
import com.hardcopy.eventflow.utils.LockUtil
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

/**
 * Used for managing topic-processor map.
 *
 * Created by godstale@hotmail.com(Young-bae Suh)
 */

class EventBus {
    private val TAG = "EventBus"
    private val processorMap = ConcurrentHashMap<String, EventProcessor>()
    private val processorLock = ReentrantLock()

    /**
     * Put the pair of topic name and EventProcessor to the HashMap.
     * Before registration check topic is already exist in the hash map.
     * If specified topic is empty, make new PublishProcessor and serialize it
     * for thread-safety.
     *
     * @param   eventProcessor      Topic and SharedFlow info holder. made by EventFlowBuilder
     * @return  EventProcessor
     */

    fun registerEventProcessor(eventProcessor: EventProcessor): EventProcessor {
        return LockUtil.lock(processorLock) {
            var result = processorMap[eventProcessor.topic.topic]
            if(result == null) {
                // create processor
                MutableSharedFlow<Any>(replay = 0,
                                       eventProcessor.getBufferSize(),
                                       onBufferOverflow = BufferOverflow.DROP_OLDEST).let {
                    eventProcessor.setSharedFlow(it)
                }
                // add to hash map
                processorMap[eventProcessor.topic.topic] = eventProcessor
                result = eventProcessor
            }

            if(EventFlowDebugUtil.isDebugMode())
                EventFlowDebugUtil.printTopics(processorMap.keys())

            result
        }
    }

    /**
     * @Deprecated  Remove must work in recursive manner.
     * Removes single topic and EventProcessor pair from hash map.
     */
    @Deprecated("Not available. Use removeEventProcessors() instead.", ReplaceWith("removeEventProcessors(topic)"))
    fun removeEventProcessor(topic: String): Boolean {
        return LockUtil.lock(processorLock) {
            val removed = processorMap.remove(topic)?.apply {
                // stop processor
                stopProcessor()
            }
            if(EventFlowDebugUtil.isDebugMode())
                EventFlowDebugUtil.printTopics(processorMap.keys())

            removed != null
        }
    }

    /**
     * Removes specified topic and descendants.
     * All subscriber on this processor would receive onComplete message.
     *
     * @param   topic       topic string.
     */
    fun removeEventProcessors(topic: String) {
        LockUtil.lock(processorLock) {
            searchEventProcessors(topic).forEach {
                processorMap.remove(it.key)
                // stop processor
                it.value.stopProcessor()
            }
            if(EventFlowDebugUtil.isDebugMode())
                EventFlowDebugUtil.printTopics(processorMap.keys())
        }
    }

    /**
     * Search the hash map with topic string and returns an EventProcessor
     *
     * @param   topic       topic string.
     * @return  EventProcessor?
     */
    fun getProcessor(topic: String): EventProcessor? {
        val eventProcessor = searchEventProcessor(topic)
        if(eventProcessor?.getSharedFlow() != null)
            return eventProcessor
        return null
    }

    /**
     * Publish event to the topic and descendants.
     * To publish to all the descendants, set recursive param as true
     *
     * @param   topic       topic string.
     */
    fun publishEvent(topic: String, contents: Any, recursive: Boolean = false) {
        if(recursive) {
            // publish event to target topic and descendants
            searchEventProcessors(topic).forEach {
                EventFlowDebugUtil.printPublishEvent(it.key, it.value, true)
                it.value.apply {
                    if(hasSubscribers()) {
                        publish(contents)
                    } else {
                        if(EventFlowDebugUtil.isDebugMode())
                            Log.d(TAG, "### topic($topic) has no subscriber.")
                    }
                }
                it.value.getSharedFlow()?.apply {

                }
            }
        } else {
            // publish event to the target topic only
            processorMap[topic]?.apply {
                EventFlowDebugUtil.printPublishEvent(topic, this)
                if(hasSubscribers()) {
                    publish(contents)
                }
            }
        }
    }

    /**
     * Search hash map and returns single EventProcessor
     * which is exactly match with topic string.
     *
     * @param   topic       topic string.
     * @return  EventProcessor
     */
    private fun searchEventProcessor(topic: String): EventProcessor? {
        return processorMap[topic]
    }

    /**
     * Search hash map with topic string and returns target processor
     * and his descendants.
     *
     * @param   topic       topic string.
     * @return  Map<String, EventProcessor>
     */
    private fun searchEventProcessors(topic: String): Map<String, EventProcessor> {
        return processorMap.filterKeys { it.startsWith(topic) }
    }

}
