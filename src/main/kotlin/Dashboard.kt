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
            column("DATE", BankTransaction::date)
            column("AMOUNT", BankTransaction::amount)
            column("MEMO", BankTransaction::memo)
            column("CATEGORY", BankTransaction::categoryProperty).useComboBox(categories.observable())

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
        }
    }
}