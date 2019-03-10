package net.corda.training.state

import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import java.util.*

/**
 * This is where you'll add the definition of your state object. Look at the unit tests in [IOUStateTests] for
 * instructions on how to complete the [IOUState] class.
 *
 * Remove the "val data: String = "data" property before starting the [IOUState] tasks.
 */
data class IOUState(
                    val amount: Amount<Currency> = Amount(0L,Currency.getInstance(Locale.US))
                    ,val lender: Party
                    ,val borrower: Party
                    ,val paid: Amount<Currency> = Amount(0L,Currency.getInstance(Locale.US))
                    , override val linearId: UniqueIdentifier = UniqueIdentifier()
                    ): ContractState, LinearState {
    override val participants: List<Party> get() = listOf(lender, borrower)

    fun pay(paid: Amount<Currency>): IOUState {
        return copy(this.amount, this.lender, this.borrower, paid + this.paid)
    }

    fun withNewLender(newLender: Party): IOUState {
        return copy(this.amount, newLender, this.borrower, this.paid)
    }
}