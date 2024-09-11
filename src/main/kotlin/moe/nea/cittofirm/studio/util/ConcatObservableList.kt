package moe.nea.cittofirm.studio.util

import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.transformation.TransformationList

fun <T> ObservableList<T>.withPrepended(vararg element: T): ObservableList<T> {
	return ConcatObservableList(element.toList(), this)
}

class ConcatObservableList<T>(
	val constPrepend: List<T>,
	origin: ObservableList<out T>,
) : TransformationList<T, T>(origin) {
	override fun get(index: Int): T {
		if (index in constPrepend.indices)
			return constPrepend[index]
		return source[index - constPrepend.size]
	}

	override fun getSourceIndex(p0: Int): Int {
		return p0 + constPrepend.size
	}

	override fun getViewIndex(p0: Int): Int {
		return p0 - constPrepend.size
	}

	override val size: Int
		get() = source.size + constPrepend.size

	override fun sourceChanged(change: ListChangeListener.Change<out T>) {
		fireChange(object : ListChangeListener.Change<T>(this@ConcatObservableList) {
			override fun next(): Boolean {
				return change.next()
			}

			override fun reset() {
				change.reset()
			}

			override fun getFrom(): Int {
				return change.from + constPrepend.size
			}

			override fun getTo(): Int {
				return change.to + constPrepend.size
			}

			override fun getRemoved(): List<T> {
				return change.removed
			}

			override fun getAddedSize(): Int {
				return change.getAddedSize()
			}

			override fun getRemovedSize(): Int {
				return change.getRemovedSize()
			}

			override fun getPermutation(p0: Int): Int {
				return change.getPermutation(p0)
			}

			override fun wasPermutated(): Boolean {
				return change.wasPermutated()
			}

			override fun wasAdded(): Boolean {
				return change.wasAdded()
			}

			override fun wasRemoved(): Boolean {
				return change.wasRemoved()
			}

			override fun wasReplaced(): Boolean {
				return change.wasReplaced()
			}

			override fun wasUpdated(): Boolean {
				return change.wasUpdated()
			}
			override fun getPermutation(): IntArray {
				return IntArray(change.to - change.from) { change.getPermutation(it + change.from) + constPrepend.size }
			}
		})
	}
}