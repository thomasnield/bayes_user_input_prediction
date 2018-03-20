import javafx.collections.FXCollections
import tornadofx.getProperty
import tornadofx.property
import java.lang.Math.exp
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.ln


val categories = listOf("Grocery", "Utility", "Coffee", "Restaurants", "Travel", "Salary")
val transactions = FXCollections.observableArrayList<BankTransaction>()
val k = .5

class BankTransaction(
        val date: LocalDate,
        val amount: BigDecimal,
        val memo: String,
        category: String? = null
) {
    var category by property<String?>(category?:estimatedCategory)
    fun categoryProperty() = getProperty(BankTransaction::category)

    val estimatedCategory get() = likelyCategoryFor(this)

    val words = memo.splitWords().toSet()
}

data class WordProbability(val word: String, val category: String, val probBelongsToCategory: Double, val notProbBelongsToCategory: Double)

data class CombinedProbability(val category: String, val probability: Double)

fun likelyCategoryFor(bankTransaction: BankTransaction): String? {

    val wordsWithProbability = transactions.asSequence()
            .filter { it.category != null }
            .flatMap { t ->
                t.memo.splitWords()
            }.distinct()
            .flatMap { w ->
                categories.asSequence()
                        .map { c ->
                            WordProbability(word=w,
                                    category = c,
                                    probBelongsToCategory = (k + transactions.count { it.category == c && w in it.words  }.toDouble()) /
                                            ((2*k) + transactions.count { it.category == c }.toDouble()),

                                    notProbBelongsToCategory = (k + transactions.count { it.category != c && w in it.words  }.toDouble()) /
                                            ((2*k) + transactions.count { it.category != c }.toDouble())
                                    )
                        }
            }.toList()

    val memoWords = bankTransaction.memo.splitWords().toSet()

    return categories.asSequence().map { c ->
        val probIfCategory = wordsWithProbability.asSequence().filter { it.category == c }.map {
            if (it.word in memoWords) {
                ln(it.probBelongsToCategory)
            } else {
                ln(1.0 - it.probBelongsToCategory)
            }
        }.sum().let(::exp)

        val probIfNotCategory = wordsWithProbability.asSequence().filter { it.category == c }.map {
            if (it.word in memoWords) {
                ln(it.notProbBelongsToCategory)
            } else {
                ln(1.0 - it.notProbBelongsToCategory)
            }
        }.sum().let(::exp)

        CombinedProbability(category = c, probability = probIfCategory / (probIfCategory + probIfNotCategory))
    }.sortedByDescending { it.probability }
     .map { it.category }
     .firstOrNull()

}

fun String.splitWords() =  split(Regex("\\s")).asSequence()
        .map { it.replace(Regex("[^A-Za-z]"),"").toLowerCase() }
        .filter { it.isNotEmpty() }