package com.hardcopy.eventflow

import com.hardcopy.eventflow.eventbus.*

/**
 * Builder class to make new topic with various options.
 *
 * Created by godstale@hotmail.com(Young-bae Suh)
 */
class EventFlowBuilder(private val eventBus: EventBus) {
    companion object {
        fun create(eventBus: EventBus): EventFlowBuilder {
            return EventFlowBuilder(eventBus)
        }
    }

    private var topicString: String = ""
    private var backpressureType = EventFlowControl.BpType.DROP_OLDEST
    private var bufferSize = EventFlowControl.DEFAULT_BUFFER_SIZE
    private var useValve = false

    // topic is fixed by setClassTopic() or setUiTopic()
    private var topicPinned = false


    /**
     * Set topic path string.
     *
     * @param   topicPath
     * @return  EventFlowBuilder
     */
    fun setTopic(topicPath: String): EventFlowBuilder {
        if(!topicPinned) {
            topicString = topicPath
        }
        return this
    }

    /**
     * Set topic with class type. EventFlow converts class type to topic path string.
     * ex> com.exam.event.MainActivity -> /sys/class/com.exam.event.MainActivity
     *
     * @param   clazz
     * @return  EventFlowBuilder
     */
    fun setTopic(clazz: Class<*>): EventFlowBuilder {
        if(!topicPinned) {
            topicString = EventTopic.TOPIC_PATH_CLASS + clazz.canonicalName!!.toString()
            topicPinned = true
        }
        return this
    }

    /**
     * Set backpressure strategy. Default is EventFlowControl.BpType.DROP_OLDEST.
     *
     * @param   type
     * @return  EventFlowBuilder
     */
    fun withBackpressure(type: EventFlowControl.BpType): EventFlowBuilder {
        backpressureType = type
        return this
    }

    /**
     * Attach valve to topic stream.
     *
     * @return  EventFlowBuilder
     */
    fun withValve(): EventFlowBuilder {
        useValve = true
        return this
    }

    /**
     * Set buffer size
     *
     * @param   size                buffer size
     * @return  EventFlowBuilder
     */
    fun setBufferSize(size: Int): EventFlowBuilder {
        if(0 < size && size < EventFlowControl.MAX_BUFFER_SIZE) {
            bufferSize = size
        }
        return this
    }

    /**
     * Make EventProcessor instance and hand over to EventBus to make Rx stream, PublishProcessor.
     *
     * @return  Boolean
     */
    fun build(): Boolean {
        // Check topic validation
        if(!EventTopic.isValidForRegister(topicString)) return false

        // Make flow controller
        val eventFlowControl = EventFlowControl(backpressureType, useValve, bufferSize)

        EventProcessor(EventTopic(topicString), eventFlowControl).let {
            eventBus.registerEventProcessor(it)
        }
        return true
    }
}