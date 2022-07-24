package com.hardcopy.eventflow.utils

/**
 * String toolkit.
 *
 * Created by godstale@hotmail.com(Young-bae Suh)
 */
object StringUtil {
    // for topic string validation check
    val topicRegEx = "^[._0-9a-zA-Z/-]*$"
    // check not visible characters
    val emptyCharRegEx = "\\p{Z}"
}