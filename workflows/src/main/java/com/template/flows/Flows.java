package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByName;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.template.contracts.DataContract;
import com.template.states.DataState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.r3.corda.lib.accounts.workflows.services.AccountService;


public class Flows {

    @InitiatingFlow
    @StartableByRPC
    public static class InitiatorFlow extends FlowLogic<SignedTransaction> {

        private String dataString;
        private String srcAccountName;
        private String dstAccountName;

        public InitiatorFlow(String dataString, String srcAccountName, String dstAccountName) {
            this.dataString = dataString;
            this.srcAccountName = srcAccountName;
            this.dstAccountName = dstAccountName;
        }

        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Task #1: Get AccountInfo objects from Account Name
            AccountInfo srcAccountInfo = null;
            AccountInfo dstAccountInfo = null;

            // Task #2: Request relevant Signing Keys
            AnonymousParty srcAnonParty = null;
            AnonymousParty dstAnonParty = null;

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            final TransactionBuilder txBuilder = new TransactionBuilder(notary);

            // Task #3: Add both Account signatures to the Command & Add both AnonymousParty objects to the DataState
            final DataState dataState = null;
            txBuilder.addCommand(new DataContract.Commands.Create()); // add required signing keys here
            txBuilder.addOutputState(dataState);

            // Task #4: Sign & Request Signature on Transaction
            final SignedTransaction ptx = null;
            final FlowSession dstSession = null;
            final SignedTransaction stx = null;
            return subFlow(new FinalityFlow(stx, dstSession));
        }
    }


    @InitiatedBy(Flows.InitiatorFlow.class)
    public static class ResponderFlow extends FlowLogic<SignedTransaction>{
        private final FlowSession flowSession;

        public ResponderFlow(FlowSession flowSession){
            this.flowSession = flowSession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow) { super(otherPartyFlow); }
                @Override protected void checkTransaction(SignedTransaction stx) { }
            }
            final SignTxFlow signTxFlow = new SignTxFlow(flowSession);
            final SecureHash txId = subFlow(signTxFlow).getId();
            return subFlow(new ReceiveFinalityFlow(flowSession, txId));
        }
    }
}