import javafx.application.Application
import javafx.geometry.Orientation
import tornadofx.App
import tornadofx.View
import tornadofx.*


fun main(args: Array<String>) = Application.launch(*args, TransactionApp::class)

class TransactionApp: App(TransactionView::class)

class TransactionView: View() {

    override val root = borderpane {
        center = tableview(transactions) {
            column("DATE", BankTransaction::date)
            column("AMOUNT", BankTransaction::amount)
            column("MEMO", BankTransaction::memo)
            column("CATEGORY", BankTransaction::categoryProperty).useComboBox(categories.observable())

            isEditable = true
        }

        left = toolbar {
            orientation = Orientation.VERTICAL

            button("+") {
                setOnAction {

                }
            }
        }
    }
}