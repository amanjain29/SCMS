package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.DistributorContract
import com.template.states.DistributorState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class ProductIssueInitiator(
        val sender: Party,
        val receiver: Party,
        val location: String
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(DistributorContract.Commands.Issue(), listOf(sender,receiver).map { it.owningKey })
        val distributorState = DistributorState(sender,receiver,location, UniqueIdentifier())
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(distributorState, DistributorContract.ID)
                .addCommand(command)

        txBuilder.verify(serviceHub)
        val tx = serviceHub.signInitialTransaction(txBuilder)
        val sessions = (distributorState.participants - ourIdentity).map { initiateFlow(it as Party) }
        val stx = subFlow(CollectSignaturesFlow(tx, sessions))
        return subFlow(FinalityFlow(stx, sessions))
        // Initiator flow logic goes here.
    }
}

@InitiatedBy(ProductIssueInitiator::class)
class ProductIssueResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "The output must be a CarState" using (output is DistributorState)
            }
        }
        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSignedId.id))
    }
        // Responder flow logic goes here.
    }
