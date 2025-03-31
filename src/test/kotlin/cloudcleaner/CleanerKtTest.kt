@file:OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)

package cloudcleaner

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.should
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class CleanerKtTest {
    @Test
    fun `produceDeletableResources should return empty list if no resources are found`() = runTest {
        coroutineScope {
            val deletableResources = produceDeletableResources(emptyMap())

            deletableResources.shouldBeEmpty()
        }
    }

    @Test
    fun `produceDeletableResources should return resources that no one depends on by dependsOn`() = runTest {
        coroutineScope {
            // A
            // B
            // C -> A
            // D -> B
            // E -> D -> B
            val a = TestResource("A")
            val b = TestResource("B")
            val c = TestResource("C", dependsOn = setOf("A"))
            val d = TestResource("D", dependsOn = setOf("B"))
            val e = TestResource("E", dependsOn = setOf("D"))
            val map = listOf(a, b, c, d, e).associateBy { it.id }

            val deletableResources = produceDeletableResources(map)

            val receivedElements = setOf(
                deletableResources.receive(),
                deletableResources.receive()
            )
            deletableResources.shouldBeEmpty()
            receivedElements shouldContainAll listOf(e, e)
        }
    }
    @Test
    fun `produceDeletableResources should return resources that no one depends on by dependsOn and contains`() = runTest {
        coroutineScope {
            // A
            // B
            // C -> A (contains 1)
            // D -> B
            // E -> D -> B
            // F -> 1
            val a = TestResource("A")
            val b = TestResource("B")
            val c = TestResource("C", dependsOn = setOf("A"), contains = setOf("1"))
            val d = TestResource("D", dependsOn = setOf("B"))
            val e = TestResource("E", dependsOn = setOf("D"))
            val f = TestResource("F", dependsOn = setOf("1"))
            val map = listOf(a, b, c, d, e, f).associateBy { it.id }

            val deletableResources = produceDeletableResources(map)

            val receivedElements = setOf(
                deletableResources.receive(),
                deletableResources.receive()
            )
            deletableResources.shouldBeEmpty()
            receivedElements shouldContainAll listOf(e, f)
        }
    }
}

fun <T> ReceiveChannel<T>.shouldBeEmpty() = this should beEmpty()
fun <T> beEmpty() = object : Matcher<ReceiveChannel<T>> {
    override fun test(value: ReceiveChannel<T>) = MatcherResult(
        value.isEmpty,
        { "ReceiveChannel should be empty" },
        { "ReceiveChannel should not be empty" }
    )
}

fun <T> ReceiveChannel<T>.shouldBeClosed() = this should beClosed()
fun <T> beClosed() = object : Matcher<ReceiveChannel<T>> {
    override fun test(value: ReceiveChannel<T>) = MatcherResult(
        value.isClosedForReceive,
        { "Channel should be closed" },
        { "Channel should not be closed" }
    )
}