**Loan Management Smart Contract (Hyperledger Fabric, Java)**

**Overview**

This project is a Loan Management System built on Hyperledger Fabric using Java chaincode.
It demonstrates how blockchain can be used in financial services to manage loan lifecycle events in a transparent, tamper-proof, and auditable way.

**Features**


    Request a new loan (requestLoan)
    
    Initialize demo data (initLedger)
    
    Query loan by ID (getLoan)
    
    Query all loans by borrower (queryLoansByBorrower)
    
    Support for private data collection (PII) via transient fields
    
    Emits blockchain events (e.g., LoanRequested)

**Project Structure**

gradle/

 └── lib/src/main/java/_LoanManagement
 
     └── LoanContract.java   # Main chaincode
     

**Deployment**

**1. Build the chaincode**

cd LoanManagement

./gradlew installDist


Output JAR will be in build/install/LoanManagement/lib/.

**2. Package the chaincode**

From your Fabric network root (e.g., fabric-samples/test-network):

peer lifecycle chaincode package loancc.tar.gz \
  --path ../eclipse-workspace/LoanManagement \
  --lang java \
  --label loancc_1

**3. Install on peers**

peer lifecycle chaincode install loancc.tar.gz

**4. Query installed**

peer lifecycle chaincode queryinstalled


Copy the package ID.

**5. Approve definition (Org1 & Org2)**

peer lifecycle chaincode approveformyorg \
  -o localhost:7050 \
  --ordererTLSHostnameOverride orderer.example.com \
  --tls $CORE_PEER_TLS_ENABLED \
  --cafile $ORDERER_CA \
  --channelID mychannel \
  --name loancc \
  --version 1.0 \
  --package-id <your-package-id> \
  --sequence 1 \
  --init-required


Repeat for both Org1 and Org2.

**6. Commit definition**

peer lifecycle chaincode commit \
  -o localhost:7050 \
  --ordererTLSHostnameOverride orderer.example.com \
  --tls $CORE_PEER_TLS_ENABLED \
  --cafile $ORDERER_CA \
  -C mychannel \
  --name loancc \
  --version 1.0 \
  --sequence 1 \
  --init-required \
  --peerAddresses localhost:7051 --tlsRootCertFiles $CORE_PEER_TLS_ROOTCERT_FILE_ORG1 \
  --peerAddresses localhost:9051 --tlsRootCertFiles $CORE_PEER_TLS_ROOTCERT_FILE_ORG2

**7. Initialize ledger**

peer chaincode invoke \
  -o localhost:7050 \
  --ordererTLSHostnameOverride orderer.example.com \
  --tls $CORE_PEER_TLS_ENABLED \
  --cafile $ORDERER_CA \
  -C mychannel -n loancc \
  -c '{"Args":["initLedger"]}'

**Testing**

Add new loan
peer chaincode invoke \
  -o localhost:7050 \
  --ordererTLSHostnameOverride orderer.example.com \
  --tls $CORE_PEER_TLS_ENABLED \
  --cafile $ORDERER_CA \
  -C mychannel -n loancc \
  -c '{"Args":["requestLoan","LOAN3","BORR3","PLN-EDU","INR","250000","24","FIXED"]}'

**Get loan by ID**

peer chaincode query -C mychannel -n loancc -c '{"Args":["getLoan","LOAN1"]}'

**Get loans by borrower**

peer chaincode query -C mychannel -n loancc -c '{"Args":["queryLoansByBorrower","BORR1"]}'

**Next Steps**

    Add underwriting workflow (approve/reject)
    
    Add loan disbursement and repayment tracking
    
    Add audit/regulator view
    
    Add unit tests and client SDK integration
