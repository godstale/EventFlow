package com.hardcopy.eventflow.exceptions

import java.lang.RuntimeException

/**
 * Created by godstale@hotmail.com(Young-bae Suh)
 */

enum class EventFlowException(val exception: RuntimeException) {
    NOT_INITIALIZED(EventFlowNotInitializedException()),
    OBSERVE_FAILED(EventFlowNotObservableException()),
    TYPE_CASTING_FAILED(EventFlowTypeCastingException())
}