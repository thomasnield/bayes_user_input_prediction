import javafx.collections.FXCollections
import java.lang.Math.exp
import java.time.LocalDate
import kotlin.math.ln


val categories = FXCollections.observableArrayList("Grocery", "Utility", "Electronics", "Entertainment", "Coffee", "Restaurants","Travel")
val transactions = FXCollections.observableArrayList<BankTransaction>()
val k = .1

class BankTransaction(
        val date: LocalDate,
        val amount: Double,
        val memo: String,
        category: String? = null
) {
    // default category to a predicated category if it is not provided
    var category  = category?:likelyCategoryFor(this)

    val words = memo.splitWords().toSet()
}

data class WordProbability(val word: String, val category: String, val probBelongsToCategory: Double, val notProbBelongsToCategory: Double)

data class CombinedProbability(val category: String, val probability: Double)

fun likelyCategoryFor(bankTransaction: BankTransaction): String? {

    val wordsWithProbability = transactions.asSequence()
            .filter { it.category != null } // only process transactions with labeled categories
            .flatMap { t ->
                t.memo.splitWords()
            }.distinct() // only process distinct words
            .flatMap { w ->

                // for each category, calculate each word's probability/not probability that it appears in that category
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
            }.toList() // collect probabilities into a list

    // distinct the words in this bank transaction
    val memoWords = bankTransaction.memo.splitWords().toSet()

    return categories.asSequence()
            .filter { c ->  transactions.count { it.category == c} > 0 && wordsWithProbability.any { it.word in memoWords } }
            .map { c ->
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
    }.filter { it.probability >= .1 }
     .sortedByDescending { it.probability }
     .onEach { println(bankTransaction.memo.trim() + " -> " + it.category + " " + it.probability) }
     .map { it.category }
     .firstOrNull()

}

fun String.splitWords() =  split(Regex("\\s")).asSequence()
        .map { it.replace(Regex("[^A-Za-z]"),"").toLowerCase() }
        .filter { it.isNotEmpty() }