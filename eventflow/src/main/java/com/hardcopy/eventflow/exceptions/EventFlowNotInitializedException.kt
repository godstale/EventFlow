package com.hardcopy.eventflow.exceptions

import java.lang.RuntimeException

/**
 * Created by godstale@hotmail.com(Young-bae Suh)
 */

class EventFlowNotInitializedException(
        message: String = "EventFlow is not initialized. Call EventFlow.initialize() first.")
    : RuntimeException(message) {}