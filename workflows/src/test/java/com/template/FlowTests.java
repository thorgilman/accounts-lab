package com.template;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount;
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfoHandler;
import com.template.flows.Flows;
import com.template.states.DataState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.node.Corda;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;
    private StateAndRef<AccountInfo> accountInfoRefA;
    private StateAndRef<AccountInfo> accountInfoRefB;

    @Before
    public void setup() throws ExecutionException, InterruptedException {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("com.template.contracts"),
                TestCordapp.findCordapp("com.template.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows"),
                TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts")
                )));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            node.registerInitiatedFlow(ShareAccountInfoHandler.class);
        }
        network.runNetwork();

        Party partyA = a.getInfo().getLegalIdentities().get(0);
        Party partyB = b.getInfo().getLegalIdentities().get(0);

        CordaFuture<StateAndRef<? extends AccountInfo>> future1 = a.startFlow(new CreateAccount("AccountA"));
        network.runNetwork();
        accountInfoRefA = (StateAndRef<AccountInfo>) future1.get();

        CordaFuture<StateAndRef<? extends AccountInfo>> future2 = b.startFlow(new CreateAccount("AccountB"));
        network.runNetwork();
        accountInfoRefB = (StateAndRef<AccountInfo>) future2.get();

        CordaFuture future3 = a.startFlow(new ShareAccountInfo(accountInfoRefA, Collections.singletonList(partyB)));
        network.runNetwork();
        future3.get();

        CordaFuture future4 = b.startFlow(new ShareAccountInfo(accountInfoRefB, Collections.singletonList(partyA)));
        network.runNetwork();
        future4.get();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    /**
     * Run this test in order to test your solution
     */
    @Test
    public void testSolution() throws ExecutionException, InterruptedException {
        CordaFuture future = a.startFlow(new Flows.InitiatorFlow("My Data", "AccountA", "AccountB"));
        network.runNetwork();
        SignedTransaction stx = (SignedTransaction) future.get();

        assert (stx.getMissingSigners().size() == 0);
        assert (stx.getSigs().size() == 2);

        assert (a.getServices().getVaultService().queryBy(DataState.class).getStates().size() == 1);
        assert (b.getServices().getVaultService().queryBy(DataState.class).getStates().size() == 1);
    }

}
