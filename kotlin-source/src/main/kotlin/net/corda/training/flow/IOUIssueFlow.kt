package net.corda.training.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.training.contract.IOUContract
import net.corda.training.state.IOUState


/**
 * This is the flow which handles issuance of new IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class IOUIssueFlow(val state: IOUState) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        var transactionBuilder = TransactionBuilder()
        transactionBuilder.notary = notary
        val issueCommand = IOUContract.Commands.Issue()
        val pKeys = state.participants.map { it.hashCode() to it.owningKey }.toMap().values.toList()
        transactionBuilder = transactionBuilder.withItems(Command(issueCommand, pKeys), StateAndContract(state, IOUContract.IOU_CONTRACT_ID))
        transactionBuilder.verify(serviceHub)
        val partlySignedTx = serviceHub.signInitialTransaction(
                transactionBuilder
        )

        // Creating a session with the other party.
        val otherpartySession = initiateFlow(state.borrower)
        // Obtaining the counterparty's signature.
        val fullySignedTx = subFlow(CollectSignaturesFlow(partlySignedTx, listOf(otherpartySession),
                CollectSignaturesFlow.tracker()))

        // Finalising the transaction.
        return subFlow(FinalityFlow(fullySignedTx))

    }
}

/**
 * This is the flow which signs IOU issuances.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(IOUIssueFlow::class)
class IOUIssueFlowResponder(val flowSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction" using (output is IOUState)
            }
        }
        subFlow(signedTransactionFlow)
    }
}