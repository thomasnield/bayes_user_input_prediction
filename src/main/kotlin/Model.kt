import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import org.apache.commons.math3.distribution.NormalDistribution
import org.nield.kotlinstatistics.randomFirst
import org.nield.kotlinstatistics.toNaiveBayesClassifier
import java.time.LocalDate
import kotlin.math.exp
import kotlin.math.ln


val categories = FXCollections.observableArrayList("Grocery", "Utility", "Electronics", "Entertainment", "Coffee", "Restaurants","Travel")
val transactions = FXCollections.observableArrayList<BankTransaction>()

val selectedClassifier = SimpleObjectProperty(ClassifierImplementation.NAIVE_BAYES)

class BankTransaction(
        val date: LocalDate,
        val amount: Double,
        val memo: String,
        category: String? = null
) {
    // default category to a predicated category if it is not provided
    var category  = category?:selectedClassifier.get().predict(this)
}


enum class ClassifierImplementation {
    NAIVE_BAYES {
        override fun predict(bankTransaction: BankTransaction): String? {
            val classifier = transactions.toNaiveBayesClassifier(
                    featuresSelector = { it.memo.discretizeWords() },
                    categorySelector = { it.category }
            )

            return classifier.predict(bankTransaction.memo.discretizeWords())
        }
    },
    LOGISTIC_REGRESSION {

        override fun predict(bankTransaction: BankTransaction): String? {

            val normalDistribution = NormalDistribution(0.0, 1.0)

            val words = bankTransaction.memo.discretizeWords().toList().toTypedArray()

            return categories.asSequence()
                    .map { category ->

                        var bestLikelihood = -10_000_000.0
                        var b0 = .01
                        val bX = words.asSequence().map { it to .01 }.toMap().toMutableMap()

                        fun sumBx(inputVariables: Map<String,Boolean>) =
                                words.asSequence()
                                        .map { bX[it]!! * (if (inputVariables[it]!!) 1.0 else 0.0) }
                                        .sum()

                        fun predictProbability(inputVariables: Map<String,Boolean>) =
                                1.0 / (1 + exp(-(b0 + sumBx(inputVariables))))

                        fun predictProbability(inputWords: Set<String>) =
                                words.asSequence()
                                        .map { w -> w to (w in inputWords) }
                                        .toMap()
                                        .let { predictProbability(it) }

                        repeat(10000) {

                            val selectedBeta = (0..bX.count()).asSequence().randomFirst()

                            val adjust = normalDistribution.sample()

                            // make random adjustment to two of the colors
                            when {
                                selectedBeta == 0 -> b0 += adjust
                                else -> bX.compute(words[selectedBeta-1]!!) { key, oldValue -> oldValue!! + adjust }
                            }

                            // calculate maximum likelihood
                            val trueEstimates = transactions.asSequence()
                                    .filter { t ->  words.any { it in t.memo.discretizeWords() } && t.category == category }
                                    .map { ln(predictProbability(it.memo.discretizeWords())) }
                                    .sum()

                            val falseEstimates = transactions.asSequence()
                                    .filter { t ->  words.any { it in t.memo.discretizeWords() } && t.category != category }
                                    .map {  ln(1 - predictProbability(it.memo.discretizeWords())) }
                                    .sum()

                            val likelihood = trueEstimates + falseEstimates

                            if (bestLikelihood < likelihood) {
                                bestLikelihood = likelihood
                            } else {
                                // revert if no improvement happens
                                when {
                                    selectedBeta == 0 -> b0 -= adjust
                                    else -> bX.compute(words[selectedBeta-1]!!) { key, oldValue -> oldValue!! - adjust }
                                }
                            }
                        }
                        category to ln(bestLikelihood)
                    }.sortedByDescending { it.second }
                    .onEach { println(it) }
                    .filter { it.second > .10 }
                    .firstOrNull()?.first
        }
    };

    abstract fun predict(bankTransaction: BankTransaction): String?

    override fun toString() = name.replace("_", " ")
}

fun String.discretizeWords() =  split(Regex("\\s")).asSequence()
        .map { it.replace(Regex("[^A-Za-z]"),"").toLowerCase() }
        .filter { it.isNotEmpty() }
        .toSet()