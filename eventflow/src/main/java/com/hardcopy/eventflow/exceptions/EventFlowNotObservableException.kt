package com.hardcopy.eventflow.exceptions

import java.lang.RuntimeException

/**
 * Created by godstale@hotmail.com(Young-bae Suh)
 */

class EventFlowNotObservableException(
        message: String = "Cannot observe topic.")
    : RuntimeException(message) {}