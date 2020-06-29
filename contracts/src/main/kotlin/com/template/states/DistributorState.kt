package com.template.states

import com.template.contracts.DistributorContract
import com.template.contracts.TemplateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

// *********
// * State *
// *********
@BelongsToContract(DistributorContract::class)
data class DistributorState(
        val sender: Party,
        val receiver: Party,
        val location: String,
        val linearId: UniqueIdentifier
):ContractState{
        override val participants: List<AbstractParty> = listOf(sender,receiver)}