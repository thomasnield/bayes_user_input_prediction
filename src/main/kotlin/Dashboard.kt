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

        style = "-fx-font-size: 16pt; "

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

            hbox {
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
                                style = "-fx-font-size: 16pt; "

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
                                    field {
                                        button("CLIPBOARD") {
                                            setOnAction {
                                                try {
                                                    Clipboard.getSystemClipboard().string.split(",").also {
                                                        date.value = LocalDate.parse(it[0])
                                                        amount.value = it[1].toDouble()
                                                        memo.value = it[2]
                                                    }
                                                } catch (e: Exception) {
                                                    println("Invalid clipboard input")
                                                }
                                            }
                                        }
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

                combobox(property = selectedClassifier,
                        values = ClassifierImplementation.values().toList().observable()
                )
            }
        }
    }
}