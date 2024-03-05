package com.luca009.imker.server

object ArgumentNotNullHelper {
    /**
     * Executes the specified [function], while supplying the specified [argument], as long as it is not null.
     * If the argument is null, the function call won't be executed and the [defaultValue] will be returned instead.
     */
    inline fun <T, R> requireArgumentNotNullOrDefault(argument: R?, defaultValue: T, function: (R) -> T): T {
        requireNotNull(argument) {
            return defaultValue
        }

        return function(argument)
    }
}