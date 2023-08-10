package ru.yakushevvv.mlkit.demo.ui.util

import com.google.android.gms.tasks.Task
import java.util.concurrent.CountDownLatch

/**
 * Функция для синхронного получения результата распознавания образов
 *
 * @param T тип получаемого результата
 */
fun <T> Task<T>.get(): T {
    val latch = CountDownLatch(1)
    var result: T? = null
    this.addOnSuccessListener {
        result = it
        latch.countDown()
    }
    latch.await()
    return result!!
}