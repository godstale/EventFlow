package com.hardcopy.eventflow.exceptions

import java.lang.RuntimeException

/**
 * Created by godstale@hotmail.com(Young-bae Suh)
 */

class EventFlowTypeCastingException(
        message: String = "Type casting failed.")
    : RuntimeException(message) {}