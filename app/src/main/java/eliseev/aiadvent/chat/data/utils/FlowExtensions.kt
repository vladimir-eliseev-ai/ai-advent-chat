package eliseev.aiadvent.chat.data.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Объединяет 6 потоков в один поток значений.
 */
fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R
): Flow<R> = combine(
    combine(flow, flow2, flow3, flow4, flow5) { t1, t2, t3, t4, t5 ->
        arrayOf(t1, t2, t3, t4, t5)
    },
    flow6
) { values, t6 ->
    @Suppress("UNCHECKED_CAST")
    transform(
        values[0] as T1,
        values[1] as T2,
        values[2] as T3,
        values[3] as T4,
        values[4] as T5,
        t6
    )
}

/**
 * Объединяет 7 потоков в один поток значений.
 */
fun <T1, T2, T3, T4, T5, T6, T7, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R
): Flow<R> = combine(
    combine(flow, flow2, flow3, flow4, flow5, flow6) { t1, t2, t3, t4, t5, t6 ->
        arrayOf(t1, t2, t3, t4, t5, t6)
    },
    flow7
) { values, t7 ->
    @Suppress("UNCHECKED_CAST")
    transform(
        values[0] as T1,
        values[1] as T2,
        values[2] as T3,
        values[3] as T4,
        values[4] as T5,
        values[5] as T6,
        t7
    )
}

/**
 * Объединяет 8 потоков в один поток значений.
 */
fun <T1, T2, T3, T4, T5, T6, T7, T8, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    flow8: Flow<T8>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8) -> R
): Flow<R> = combine(
    combine(flow, flow2, flow3, flow4, flow5, flow6, flow7) { t1, t2, t3, t4, t5, t6, t7 ->
        arrayOf(t1, t2, t3, t4, t5, t6, t7)
    },
    flow8
) { values, t8 ->
    @Suppress("UNCHECKED_CAST")
    transform(
        values[0] as T1,
        values[1] as T2,
        values[2] as T3,
        values[3] as T4,
        values[4] as T5,
        values[5] as T6,
        values[6] as T7,
        t8
    )
}
