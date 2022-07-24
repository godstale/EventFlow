package com.hardcopy.eventflow.eventbus

import com.hardcopy.eventflow.utils.StringUtil
import java.util.regex.Pattern

/**
 * Supply topic validation tool.
 * Holds topic presets for internal use.
 *
 * Created by godstale@hotmail.com(Young-bae Suh)
 */
class EventTopic(private val topicFullPath: String) {
    companion object {
        // Reserved by EventFlow, cannot use for custom
        const val TOPIC_ROOT = "/"                  // topic root
        const val TOPIC_SYS = "/sys"                // root of system topic
        const val TOPIC_CLASS = "/sys/class"        // Class topic - when user set class as topic name
        const val TOPIC_UI = "/sys/ui"              // Not available yet
        const val TOPIC_COMMON = "/sys/common"      // Default topic (for simple usage without topic)

        const val TOPIC_PATH_SYS = "$TOPIC_SYS/"
        const val TOPIC_PATH_CLASS = "$TOPIC_CLASS/"
        const val TOPIC_PATH_UI = "$TOPIC_UI/"
        const val TOPIC_PATH_COMMON = "$TOPIC_COMMON/"

        const val TOPIC_LENGTH_MAX = 256

        /**
         * Check topic string is valid for internal process.
         */
        private fun isValid(topicFullPath: String): Boolean {
            if(topicFullPath.isEmpty()) return false
            if(!Pattern.matches(StringUtil.topicRegEx, topicFullPath)) return false
            if(topicFullPath.contains(StringUtil.emptyCharRegEx)) return false

            // check max length
            if(topicFullPath.length > TOPIC_LENGTH_MAX) return false
            // cannot use root "/"
            if(topicFullPath == TOPIC_ROOT) return false
            // topic must start with "/"
            if(!topicFullPath.startsWith(TOPIC_ROOT)) return false
            // topic must not end with "/"
            if(topicFullPath.endsWith(TOPIC_ROOT)) return false
            // Empty topic name is not allowed
            if(topicFullPath.contains("//")) return false
            // "/sys" is internal use only
            if(topicFullPath == TOPIC_SYS) return false
            // "/sys/class" is internal use only
            if(topicFullPath == TOPIC_CLASS) return false
            // "/sys/ui" is internal use only
            if(topicFullPath == TOPIC_UI) return false

            return true
        }

        fun isValidForRegister(topicString: String): Boolean {
            if(!isValid(topicString)) return false
            return true
        }

        fun isValidForSubscribe(topicString: String): Boolean {
            if(!isValid(topicString)) return false
            return true
        }

        fun isValidForPublish(topicString: String): Boolean {
            if(!isValid(topicString)) return false
            return true
        }

        fun isValidForRemove(topicString: String): Boolean {
            if(!isValid(topicString)) return false
            if(topicString.startsWith(TOPIC_COMMON)) return false
            return true
        }

        /**
         * Makes topic string with class type.
         * EventFlow converts class type parameter into topic string like below.
         * ex> com.exam.event.MainActivity -> /sys/class/com.exam.event.MainActivity
         */
        fun getClassTopicString(clazz: Class<*>): String {
            return "$TOPIC_PATH_CLASS${clazz.canonicalName}"
        }
    }

    val topic: String = if(isValidForRegister(topicFullPath))
        topicFullPath
    else
        TOPIC_COMMON

}