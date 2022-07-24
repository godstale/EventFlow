package com.hardcopy.eventflow.eventbus

import kotlinx.coroutines.channels.BufferOverflow

/**
 * Control the back pressure and valve settings.
 *
 * Created by godstale@hotmail.com(Young-bae Suh)
 */
class EventFlowControl(private val backpresssure: BpType = BpType.DROP_OLDEST,
                       private val useValve: Boolean = true,
                       private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) {
    companion object {
        const val DEFAULT_BUFFER_SIZE = 64
        const val MAX_BUFFER_SIZE = 1024
    }

    enum class BpType(val type: BufferOverflow) {
        SUSPEND(BufferOverflow.SUSPEND),
        DROP_OLDEST(BufferOverflow.DROP_OLDEST),
        DROP_LATEST(BufferOverflow.DROP_LATEST)
    }

    var isValveOpened = true

    /**
     * Returns back pressure type.
     *
     * @return  BpType
     */
    fun getBackPressureType(): BpType {
        return backpresssure
    }

    /**
     * Returns whether valve is on or not.
     *
     * @return  Boolean
     */
    fun isValveEnabled(): Boolean {
        return useValve
    }

    /**
     * Returns buffer size.
     *
     * @return  Int
     */
    fun getBufferSize(): Int {
        return bufferSize
    }

    /**
     * Close or open the valve. Only works when useValve is true.
     *
     * @param   isOpened
     * @return  Boolean
     */
    fun switchValve(isOpened: Boolean): Boolean {
        return if(useValve) {
            isValveOpened = isOpened
            true
        } else {
            false
        }
    }
}