# Predicting User Inputs with Bayes

[Bayes' theorem](https://en.wikipedia.org/wiki/Bayes%27_theorem) is arguably one of the most-used building blocks in statistics and machine learning. While it has many complex applications, this simple formula is the backbone of so much real-world mathematical modeling we use everyday. From [diagnosing diseases](https://brilliant.org/wiki/bayesian-theory-in-science-and-math/#biomedical-test-results) to [improving time series forecasting](https://multithreaded.stitchfix.com/blog/2016/04/21/forget-arima/), Bayes has been crucial to solving many problems in the data science domain.

We are going to look at a simple but powerful usage of [Naive Bayes](https://en.wikipedia.org/wiki/Naive_Bayes_classifier), which combines several inferred probabilities of independent features into one probability. Naive Bayes is often used for text and document categorization, with the most common example being [email spam filtering](https://en.wikipedia.org/wiki/Naive_Bayes_spam_filtering). But in this article, we will see how Naive Bayes can greatly improve the usability of an app without using any data science libraries.

## The Problem

Imagine you are a software engineer. Your boss tasks you with creating a personal finance app that imports bank statements, categorizes the transactions, and reports insights on the user's spending habits.

We are going to focus on the categorizing of transactions. Initially, you build the app so the user manually categorizes the transactions like this using a combo box (Figure 1).

![](https://i.imgur.com/VOWWP6o.png)

**Figure 1- An app to input and categorize bank transactions**

However, your boss reviews your app and seems dissatisfied. After a thoughtful pause, she suddenly drops the "machine learning" buzzword on you. She would like this app to learn from the user's previous inputs and predict categories for new transactions. That way the app will look smart, be less tedious to use, and provide much more value to the user.

Rather than saying "we need to hire a data scientist", you decide to experiment a little. You speculate that perhaps you can split the MEMO text values into individual words. Then you can leverage some rudimentary probability and calculate which words occurred the most in which categories.

Your first attempts are disastrous though. You have no idea how to combine and update the probabilities. On top of that, ambiguity of keywords is rampant. The word "RENTAL" can occur in "ENTERTAINMENT" transactions (which included a "REDBOX DVD RENTAL") as well as "TRAVEL" (which included a "HERTZ CAR RENTAL"). The predicting behavior quickly becomes unpredictable and messy, but you are still determined to make it work.

After perusing an [O'Reilly book on Bayes](https://www.safaribooksonline.com/library/view/think-bayes/9781491945407/), you find that it describes your problem perfectly. You have existing data that says `A`, but then new data `B` comes in, and now you must update `A` given the new evidence `B`. However, you are unsure how to apply this simple formula in context of your current problem. You then encounter Naive Bayes, which is used to categorize text and documents based on combined probabilities of keywords. Bingo, let's get started.

## Setup

In this article I am going to use [Kotlin](https://kotlinlang.org/), an intuitive JVM language built by [JetBrains](https://www.jetbrains.com/) (the company that built Intellij IDEA, PyCharm, and other developer tools). Kotlin has rapidly rose to prominence after Google made it the [the official language for Android](https://kotlinlang.org/docs/reference/android-overview.html). You can learn all about using Kotlin for Data Science purposes in the O'Reilly video [_From Data Science to Production with Kotlin_](https://www.safaribooksonline.com/library/view/from-data-science/9781491998205/).

To quickly set up a Kotlin environment, check out [the official _Getting Started_ document](https://kotlinlang.org/docs/tutorials/getting-started.html). The only dependency you will need besides Kotlin's Standard Library is [TornadoFX](https://tornadofx.io/), a desktop UI framework for Kotlin. We will build this example as a quick desktop app.

If you do not want to use the Intellij IDEA, you also have the option of using the [command line compiler](https://kotlinlang.org/docs/tutorials/command-line.html), or an automated build system like [Maven](https://kotlinlang.org/docs/reference/using-maven.html) or [Gradle](https://kotlinlang.org/docs/reference/using-gradle.html).

Whatever means you choose to build, create a new Kotlin project.

## Laying the Foundation

In my Kotlin project, I am going to create a new file called `Model.kt`. I will import some needed types from Java and Kotlin's standard libraries. Then I will declare a list of categories represented as strings, as well as an empty list of `BankTransaction` items. The `BankTransaction` will be declared as a class with four properties: `date`, `amount`, `memo`, and `category`. The `category` can optionally be provided, and if it is `null` it will default to the result of a function called `likelyCategoryFor()`, which we will focus on in this article.

We will also introudce a `Double` smoothing constant `k`, which I will explain shortly. Here is our foundational code:

```kotlin
import javafx.collections.FXCollections
import java.lang.Math.exp
import java.time.LocalDate
import kotlin.math.ln

// A UI-friendly list of categories represented as Strings
val categories = FXCollections.observableArrayList("Grocery", "Utility", "Electronics", "Entertainment", "Coffee", "Restaurants","Travel")

// A UI-friendly empty list of bank transactions we will populate later
val transactions = FXCollections.observableArrayList<BankTransaction>()

// Smoothing constant
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

fun likelyCategoryFor(bankTransaction: BankTransaction): String?
  return null // will implement this next
}
```

Create another file which I will name `Dashboard.kt`, and this will contain my entire user interface code as well as a `main()` function to launch the application. This will create a simple `TableView` with a `Button` to add transactions. That button will trigger a dialog to input a new `BankTransaction` into our model.

```kotlin
import javafx.application.Application
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.input.Clipboard
import tornadofx.*
import java.time.LocalDate


fun main(args: Array<String>) = Application.launch(TransactionApp::class.java, *args)

class TransactionApp: App(TransactionView::class)

class TransactionView: View() {

    override val root = borderpane {

        title = "Bank Transaction Categorizer"

        center = tableview(transactions) {
            readonlyColumn("DATE", BankTransaction::date)
            readonlyColumn("AMOUNT", BankTransaction::amount)
            readonlyColumn("MEMO", BankTransaction::memo)
            column("CATEGORY", BankTransaction::category).useComboBox(categories)

            isEditable = true

            items.onChange {
                resizeColumnsToFitContent()
            }
        }

        left = toolbar {
            orientation = Orientation.VERTICAL

            button("+") {
                setOnAction {

                    val date = SimpleObjectProperty<LocalDate>()
                    val amount = SimpleDoubleProperty()
                    val memo = SimpleStringProperty()

                    val result = Dialog<BankTransaction?>().apply {
                        title = "Enter Transaction"
                        headerText = "Input a new transaction"

                        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

                        dialogPane.content = form {
                            fieldset {
                                field("DATE") {
                                    datepicker(date)
                                }
                                field("AMOUNT") {
                                    textfield(amount)
                                }
                                field("MEMO") {
                                    textfield(memo)
                                }
                            }
                        }
                        setResultConverter {
                            if (it == ButtonType.OK) {
                                BankTransaction(date.value, amount.value, memo.value)
                            } else null
                        }

                        showAndWait()
                    }.result

                    if (result != null)
                        transactions += result
                }
            }
        }
    }
}
```

You can even run this application to test it out. Click the "+" button to input a new `BankTransaction` and you should see it added to the `TableView` (Figure 2).

![](https://i.imgur.com/lBhtjws.png)
**Figure 2- Inputting transactions into the app**

However, what we are going to focus on is having a predicted category assigned to each inputted `BankTransaction`. This will be implemented in our `likelyCategoryFor()` function which will take focus for the rest of this article. 

##

$$P(C_{test})$$
