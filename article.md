# Predicting User Inputs with Bayes

[Bayes' theorem](https://en.wikipedia.org/wiki/Bayes%27_theorem) is arguably one of the most-used building blocks in statistics and machine learning. While it has many complex applications, this simple formula is the backbone of so much real-world mathematical modeling we use everyday. From [diagnosing diseases](https://brilliant.org/wiki/bayesian-theory-in-science-and-math/#biomedical-test-results) to [improving time series forecasting](https://multithreaded.stitchfix.com/blog/2016/04/21/forget-arima/), Bayes has been crucial to solving many problems in the data science domain.

In this article, we are going to look at a simple but powerful usage of [Naive Bayes](https://en.wikipedia.org/wiki/Naive_Bayes_classifier), which combines several inferred probabilities of independent features into one probability. Naive Bayes is often used for text and document categorization, with the most common example being [email spam filtering](https://en.wikipedia.org/wiki/Naive_Bayes_spam_filtering). 

In this article, we will see how Naive Bayes can greatly improve the usability of an app without using any data science libraries.

## The Problem

Imagine you are a software engineer. Your boss tasks you with creating a personal finance app that imports bank statements, categorizes the transactions, and reports insights on the user's spending habits.

We are going to focus on the categorizing of transactions. Initially, you build the app so the user manually categorizes the transactions like this using a combo box:

![](https://i.imgur.com/VOWWP6o.png)

However, your boss reviews your app and seems dissatisfied. After a thoughtful pause, she suddenly drops the "machine learning" buzzword on you. She would like this app to learn from the user's previous inputs and predict categories for new transactions. That way the app will look smart, be less tedious to use, and provide much more value to the user.

Rather than saying "we need to hire a data scientist", you decide to experiment a little. You speculate that perhaps you can split the MEMO text values into individual words. Then you can leverage some rudimentary probability and calculate which words occurred the most in which categories.

Your first attempts are disastrous though. You have no idea how to combine and update the probabilities. On top of that, ambiguity of keywords is rampant. The word "RENTAL" can occur in "ENTERTAINMENT" transactions (which included a "REDBOX DVD RENTAL") as well as "TRAVEL" (which included a "HERTZ CAR RENTAL"). The predicting behavior quickly becomes unpredictable and messy, but you are still determined to make it work.

After perusing an [O'Reilly book on Bayes](https://www.safaribooksonline.com/library/view/think-bayes/9781491945407/), you find that it describes your problem perfectly. You have existing data that says `A`, but then new data `B` comes in, and now you must update `A` given the new evidence `B`. However, you are unsure how to apply this simple formula in context of your current problem. You then encounter Naive Bayes, which is used to categorize text and documents based on combined probabilities of keywords. Bingo, let's get started. 

## Setup

In this article I am going to use [Kotlin](https://kotlinlang.org/), an intuitive JVM language built by [JetBrains](https://www.jetbrains.com/) (the company that built Intellij IDEA, PyCharm, and other developer tools). Kotlin has rapidly rose to prominence after Google made it the [the official language for Android](https://kotlinlang.org/docs/reference/android-overview.html). It works with JVM libraries including Apache Spark, so if you tried to learn Scala or Java and found it was not for you, Kotlin may be worth a look. You can learn all about using Kotlin for Data Science purposes in the O'Reilly video [_From Data Science to Production with Kotlin_](https://www.safaribooksonline.com/library/view/from-data-science/9781491998205/). 

To quickly set up a Kotlin environment, check out [the official "Getting Started" document](https://kotlinlang.org/docs/tutorials/getting-started.html). The only dependency you will need besides Kotlin std-lib 1.2.31 is [TornadoFX](https://tornadofx.io/), a desktop UI framework for Kotlin. We will build this example as a quick desktop app. 

If you do not want to use the Intellij IDEA IDE, You also have the option of using the [command line compiler](https://kotlinlang.org/docs/tutorials/command-line.html), or an automated build system like [Maven](https://kotlinlang.org/docs/reference/using-maven.html) or [Gradle](https://kotlinlang.org/docs/reference/using-gradle.html).

Whatever means you choose to build, create a new Kotlin project. Typically, you will write your source code files in a nested folder path `/src/main/kotlin/` within your project. 

## Building the Model 

In my Kotlin project, I am going to create a new file called `Model.kt`. 
