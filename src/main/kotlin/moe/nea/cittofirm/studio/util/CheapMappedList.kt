package moe.nea.cittofirm.studio.util

import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.TransformationList

fun <Old, New> ObservableList<Old>.observedCheapMap(transform: (Old) -> New): ObservableList<New> {
	return CheapMappedList(this, transform)
}

fun <T> ObservableList<T>.observedFilter(predicate: (T) -> Boolean): ObservableList<T> {
	return FilteredList(this, predicate)
}

inline fun <T, reified C : T> ObservableList<T>.observedFilterIsInstance(): ObservableList<C> {
	return FilteredList(this, { it is C }) as ObservableList<C>
}


class CheapMappedList<Old, New>(
	origin: ObservableList<out Old>,
	val mapper: (Old) -> New
) : TransformationList<New, Old>(origin) {
	override fun get(index: Int): New {
		return mapper(source[index])
	}

	override fun getSourceIndex(p0: Int): Int {
		return p0
	}

	override fun getViewIndex(p0: Int): Int {
		return p0
	}

	override val size: Int
		get() = source.size

	override fun sourceChanged(change: ListChangeListener.Change<out Old>) {
		fireChange(object : ListChangeListener.Change<New>(this@CheapMappedList) {
			override fun next(): Boolean {
				return change.next()
			}

			override fun reset() {
				change.reset()
			}

			override fun getFrom(): Int {
				return change.from
			}

			override fun getTo(): Int {
				return change.to
			}

			override fun getRemoved(): List<New> {
				return change.removed.map(mapper)
			}

			override fun getPermutation(): IntArray {
				return IntArray(to - from) { change.getPermutation(it + from) }
			}
		})
	}

}