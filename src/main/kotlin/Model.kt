import javafx.collections.FXCollections
import org.nield.kotlinstatistics.toNaiveBayesClassifier
import java.time.LocalDate


val categories = FXCollections.observableArrayList("Grocery", "Utility", "Electronics", "Entertainment", "Coffee", "Restaurants","Travel")
val transactions = FXCollections.observableArrayList<BankTransaction>()


class BankTransaction(
        val date: LocalDate,
        val amount: Double,
        val memo: String,
        category: String? = null
) {
    // default category to a predicated category if it is not provided
    var category  = category?:likelyCategoryFor(this)
}


fun likelyCategoryFor(bankTransaction: BankTransaction): String? {

    val classifier = transactions.toNaiveBayesClassifier(
            featuresSelector = { it.memo.discretizeWords() },
            categorySelector = { it.category }
    )

    return classifier.predict(bankTransaction.memo.discretizeWords())
}

fun String.discretizeWords() =  split(Regex("\\s")).asSequence()
        .map { it.replace(Regex("[^A-Za-z]"),"").toLowerCase() }
        .filter { it.isNotEmpty() }
        .toSet()