package _LoanManagement;

//package org.hyperledger.fabric.samples.loanmanagement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Arrays;

@Contract(name = "LoanContract")
@Default
public class LoanContract implements ContractInterface {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    // ----------------- Simple model -----------------
    public static class Loan {
        public String loanId;
        public String borrowerId;
        public String productCode;
        public String currency;
        public Double requestedAmount;
        public Double approvedAmount;
        public Integer termMonths;
        public String rateType;
        public String status; // REQUESTED, APPROVED, DISBURSED, ACTIVE, CLOSED, DEFAULTED
        public String createdBy;
        public String createdAt;
        public String updatedAt;
        public String piiHash; // short digest of private data if provided
    }

    // ----------------- Helpers -----------------
    private static String loanKey(String loanId) {
        return "loan:" + loanId;
    }

    private static void putObject(ChaincodeStub stub, String key, Object obj) {
        stub.putStringState(key, GSON.toJson(obj));
    }

    private static <T> T getObject(ChaincodeStub stub, String key, Class<T> clazz) {
        String s = stub.getStringState(key);
        if (s == null || s.isEmpty()) return null;
        return GSON.fromJson(s, clazz);
    }

    private static void emitEvent(ChaincodeStub stub, String name, Object payload) {
        byte[] payloadBytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        stub.setEvent(name, payloadBytes);
    }

    // ----------------- Transactions -----------------

    /**
     * Initialize ledger with some sample loans.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void initLedger(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        Loan l1 = new Loan();
        l1.loanId = "LOAN1";
        l1.borrowerId = "BORR1";
        l1.productCode = "PLN-STD";
        l1.currency = "INR";
        l1.requestedAmount = 100000.0;
        l1.termMonths = 12;
        l1.rateType = "FIXED";
        l1.status = "REQUESTED";
        l1.createdBy = "Init";
        l1.createdAt = stub.getTxTimestamp().toString();
        l1.updatedAt = l1.createdAt;
        putObject(stub, loanKey(l1.loanId), l1);

        Loan l2 = new Loan();
        l2.loanId = "LOAN2";
        l2.borrowerId = "BORR2";
        l2.productCode = "PLN-HOME";
        l2.currency = "INR";
        l2.requestedAmount = 500000.0;
        l2.termMonths = 60;
        l2.rateType = "FLOAT";
        l2.status = "REQUESTED";
        l2.createdBy = "Init";
        l2.createdAt = stub.getTxTimestamp().toString();
        l2.updatedAt = l2.createdAt;
        putObject(stub, loanKey(l2.loanId), l2);
    }

    /**
     * Create a loan request (minimal).
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String requestLoan(final Context ctx,
                              final String loanId,
                              final String borrowerId,
                              final String productCode,
                              final String currency,
                              final double amount,
                              final int termMonths,
                              final String rateType) {
        ChaincodeStub stub = ctx.getStub();

        // check exists
        if (getObject(stub, loanKey(loanId), Loan.class) != null) {
            throw new ChaincodeException("Loan already exists: " + loanId);
        }

        Loan l = new Loan();
        l.loanId = loanId;
        l.borrowerId = borrowerId;
        l.productCode = productCode;
        l.currency = currency;
        l.requestedAmount = amount;
        l.termMonths = termMonths;
        l.rateType = rateType;
        l.status = "REQUESTED";
        l.createdBy = ctx.getClientIdentity().getId();
        l.createdAt = stub.getTxTimestamp().toString();
        l.updatedAt = l.createdAt;

        // Optional: store PII in private data collection if provided
        Map<String, byte[]> tm = stub.getTransient();
        if (tm != null && tm.containsKey("pii")) {
            byte[] pii = tm.get("pii");
            String collection = "borrowerPII";
            stub.putPrivateData(collection, loanId, pii);
            byte[] digest = Arrays.copyOfRange(pii, 0, Math.min(16, pii.length));
            l.piiHash = Base64.getEncoder().encodeToString(digest);
        }

        putObject(stub, loanKey(loanId), l);
        emitEvent(stub, "LoanRequested", l);
        return GSON.toJson(l);
    }

    /**
     * Simple read-only query that returns the Loan JSON for a given loanId.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getLoan(final Context ctx, final String loanId) {
        ChaincodeStub stub = ctx.getStub();
        Loan l = getObject(stub, loanKey(loanId), Loan.class);
        if (l == null) throw new ChaincodeException("Loan not found: " + loanId);
        return GSON.toJson(l);
    }

    /**
     * Example rich query (requires CouchDB). Returns an array of loan JSONs for a borrowerId.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryLoansByBorrower(final Context ctx, final String borrowerId) {
        ChaincodeStub stub = ctx.getStub();
        String q = String.format("{\"selector\":{\"borrowerId\":\"%s\"}}", borrowerId);
        QueryResultsIterator<KeyValue> results = stub.getQueryResult(q);
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (KeyValue kv : results) {
            if (!first) sb.append(",");
            first = false;
            sb.append(kv.getStringValue());
        }
        sb.append("]");
        return sb.toString();
    }
}
