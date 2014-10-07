/**
 * Created by whoisjeremylam on 14/03/14.
 */

import org.apache.log4j.*
import org.sqlite.SQLite
import groovy.sql.Sql

@Grab(group='log4j', module='log4j', version='1.2.17')
@Grab(group='org.xerial', module='sqlite-jdbc', version='3.7.2')


public class VenndNativeFollower {
    static logger
    static log4j
    static bitcoinAPI
    static satoshi = 100000000
    static String inAsset
    static String outAsset
    static boolean outAssetDivisible
    static boolean outAssetIssuanceDependent
    static int inceptionBlock
    static BigDecimal feeAmountPercentage
    static BigDecimal txFee
    static BigDecimal refundTxFee
    static assetConfig
    static String listenerAddress
    static String paymentAddress
    static boolean testMode
    static int confirmationsRequired
    static int sleepIntervalms
    static String outAssetNonDivisibleRoundRule
    static Long outAssetMultiplier
    static String databaseName
    static db

    public class Asset {
        def String counterpartyAssetName
        def String nativeAssetName
        def String counterpartyAddress // the counterparty/bitcoin address side where we will receive the Counterparty asset
        def String nativeAddress // The address which users should send their native asset to
        def BigDecimal txFee
        def BigDecimal feePercentage
        def int inceptionBlock
        def boolean mappingRequired

        public Asset(String counterpartyAssetNameValue, String nativeAssetNameValue, String counterpartyAddressValue, String nativeAddressValue, BigDecimal txFeeValue, BigDecimal feePercentageValue, boolean mappingRequiredValue) {
            counterpartyAssetName = counterpartyAssetNameValue
            nativeAssetName = nativeAssetNameValue
            counterpartyAddress = counterpartyAddressValue
            nativeAddress = nativeAddressValue
            txFee = txFeeValue
            feePercentage = feePercentageValue
            mappingRequired = mappingRequiredValue
        }
    }

    public static class Payment {
        def String inAsset
        def Long currentBlock
        def String txid
        def String sourceAddress
        def String destinationAddress
        def String outAsset
        def Long outAmount
        def Long lastModifiedBlockId
        def String status
        def Long refundAmount = 0
        def String issuanceStatus

        public Payment(String inAssetValue, Long currentBlockValue, String txidValue, String sourceAddressValue, String destinationAddressValue, String outAssetValue, Long outAmountValue, Long lastModifiedBlockIdValue, Long originalAmount, boolean unclearSource) {
            def row
            inAsset = inAssetValue
            outAsset = outAssetValue
            outAmount = outAmountValue * outAssetMultiplier
            // Treat indivisible asset differently as they aren't multiplied by the satoshi factor
            if (!outAssetDivisible) {
                def Float outAmountFloat = outAmount / satoshi

                if (outAssetNonDivisibleRoundRule == 'floor') {
                    outAmount = Math.floor(outAmountFloat)
                    refundAmount = 0
                }
                else if (outAssetNonDivisibleRoundRule == 'ceiling') {
                    outAmount = Math.ceil(outAmountFloat)
                    refundAmount = 0
                }
                else if (outAssetNonDivisibleRoundRule == 'roundRefund') {
                    outAmount = Math.round(outAmountFloat)
                    refundAmount = 0 // to do
                }
                else {
                    outAmount = Math.round(outAmountFloat)
                    refundAmount = 0
                }

            }
            currentBlock = currentBlockValue
            txid = txidValue
            lastModifiedBlockId = lastModifiedBlockIdValue

            if (originalAmount <= 10000000) {
                status = 'authorized'
            }
            else {
                status = 'valid'
            }

            // If a precise amount of asset must be issued each time, payments must wait until the asset is issued
            if (outAssetIssuanceDependent) {
                issuanceStatus = status
                status = 'waitIssuance'
            }

            // If the source address appeared to come from a wallet which isn't Counterwallet, default the payment to manual
            if (unclearSource) {
                status = 'manual'
            }


            // Check if the send was performed TO an address registered via the API
            // If it was then payment should be swept into the central address
            row = db.firstRow("select * from addressMaps where nativePaymentAddress = ${destinationAddressValue}")
            if (row != null) {
                sourceAddress = destinationAddressValue
                destinationAddress = listenerAddress
                outAsset = inAssetValue
            }
            else {
                // Check if the send was performed FROM an address registered via the API to the central address
                // If it was then payment should be made from the central address to the external address
                row = db.firstRow ("select * from addressMaps where nativePaymentAddress = ${sourceAddressValue}")
                if (row != null) {
                    sourceAddress = listenerAddress
                    destinationAddress = row.counterpartyAddress
                    outAsset = outAsset
                }
                else {
                    // vanilla send - send from the central address the native equivalent to the address which sent this asset
                    sourceAddress = paymentAddress
                    destinationAddress = sourceAddressValue
                    outAsset = outAsset
                }
            }
        }
    }


    public init() {
        bitcoinAPI = new BitcoinAPI()

        // Set up some log4j stuff
        logger = new Logger()
        PropertyConfigurator.configure("VenndNativeFollower_log4j.properties")
        log4j = logger.getRootLogger()

        // Read in ini file
        def iniConfig = new ConfigSlurper().parse(new File("VenndNativeFollower.ini").toURL())
        inAsset = iniConfig.inAssetName
        outAsset = iniConfig.outAssetName
        outAssetDivisible = iniConfig.outAssetDivisible
        outAssetIssuanceDependent = iniConfig.outAssetIssuanceDependent
        inceptionBlock = iniConfig.inceptionBlock
        feeAmountPercentage = iniConfig.feeAmountPercentage
        txFee = iniConfig.txFee
        refundTxFee = iniConfig.refundTxFee
        testMode = iniConfig.testMode
        listenerAddress = iniConfig.listenerAddress
        paymentAddress = iniConfig.paymentAddress
        sleepIntervalms = iniConfig.sleepIntervalms
        databaseName = iniConfig.database.name
        confirmationsRequired = iniConfig.confirmationsRequired
        outAssetNonDivisibleRoundRule = iniConfig.outAssetNonDivisibleRoundRule
        outAssetMultiplier = iniConfig.outAssetMultiplier

        assetConfig = []
        iniConfig.asset.each { it ->
            def currentAsset = new Asset(it.value.counterpartyAssetName, it.value.nativeAssetName, it.value.counterpartyAddress, it.value.nativeAddress, it.value.txFee, it.value.feePercentage, it.value.mappingRequired)
            assetConfig.add(currentAsset)
        }


        // Init database
        def row
        db = Sql.newInstance("jdbc:sqlite:${databaseName}", "org.sqlite.JDBC")
        db.execute("PRAGMA busy_timeout = 1000")
        db.execute("create table if not exists blocks (blockId integer, status string, duration integer)")
        db.execute("create table if not exists transactions(blockId integer, txid string)")
        db.execute("create table if not exists credits(blockIdSource integer, txid string, sourceAddress string, destinationAddress string, inAsset string, inAmount integer, outAsset string, outAmount integer, status string)")
        db.execute("create table if not exists debits(blockIdSource integer, txid string, sourceAddress string, destinationAddress string, inAsset string, inAmount integer, outAsset string, outAmount integer, status string, lastUpdatedBlockId integer)")
        db.execute("create table if not exists inputAddresses(txid string, address string)")
        db.execute("create table if not exists outputAddresses(txid string, address string)")
        db.execute("create table if not exists fees(blockId string, txid string, feeAsset string, feeAmount integer)")
        db.execute("create table if not exists audit(blockId string, txid string, description string)")
        db.execute("create table if not exists payments(blockId integer, sourceTxid string, sourceAddress string, destinationAddress string, outAsset string, outAmount integer, status string, lastUpdatedBlockId integer)")
        db.execute("create table if not exists issuances(blockId integer, sourceTxid string, destinationAddress string, asset string, amount integer, divisibility string, status string, lastUpdatedBlockId integer)")
        db.execute("create table if not exists addressMaps (counterpartyPaymentAddress string, nativePaymentAddress string, externalAddress string, counterpartyAddress string, counterpartyAssetName string, nativeAssetName string, UDF1 string, UDF2 string, UDF3 string, UDF4 string, UDF5 string)")

        db.execute("create unique index if not exists blocks1 on blocks(blockId)")
        db.execute("create index if not exists transactions1 on transactions(blockId)")
        db.execute("create index if not exists transactions2 on transactions(txid)")
        db.execute("create index if not exists credits1 on credits(blockIdSource)")
        db.execute("create index if not exists credits2 on credits(txid)")
        db.execute("create index if not exists fees1 on fees(blockId, txid)")
        db.execute("create index if not exists inputAddresses1 on inputAddresses(txid)")
        db.execute("create index if not exists inputAddresses2 on inputAddresses(address)")
        db.execute("create index if not exists outputAddresses1 on outputAddresses(txid)")
        db.execute("create index if not exists outputAddresses2 on outputAddresses(address)")
        db.execute("create index if not exists payments1 on payments(blockId)")
        db.execute("create index if not exists payments1 on payments(sourceTxid)")
        db.execute("create index if not exists issuances1 on issuances(blockId)")
        db.execute("create unique index if not exists addressMaps1 on addressMaps(counterpartyPaymentAddress)")
        db.execute("create unique index if not exists addressMaps2 on addressMaps(nativePaymentAddress)")
        db.execute("create unique index if not exists addressMaps3 on addressMaps(externalAddress)")
        db.execute("create unique index if not exists addressMaps3 on addressMaps(counterpartyAddress)")
    }


    // Checks balances in wallet versus what is in the DB tables
    public audit() {

    }


    static public lastProcessedBlock() {
        def Long result

        def row = db.firstRow("select max(blockId) from blocks where status in ('processed','error')")

        assert row != null

        if (row[0] == null) {
            db.execute("insert into blocks values(${inceptionBlock}, 'processed', 0)")
            result = inceptionBlock
        }
        else {
            result = row[0]
        }

        assert result > 0
        return result
    }


    static public lastBlock() {
        def Long result

        def row = db.firstRow("select max(blockId) from blocks")

        assert row != null

        if (row[0] == null) {
            result = inceptionBlock
        }
        else {
            result = row[0]
        }

        return result
    }


    static processSeenBlock(currentBlock) {
        println "Block ${currentBlock}: seen"
        db.execute("insert into blocks values (${currentBlock}, 'seen', 0)")
    }


    static processBlock(currentBlock) {
        def timeStart
        def timeStop

        timeStart = System.currentTimeMillis()
        def blockHash = bitcoinAPI.getBlockHash(currentBlock)
        def block = bitcoinAPI.getBlock(blockHash)

        println "Block ${currentBlock}: processing " + block['tx'].size() + " transactions"

        def result
        def rawtransactions = []
        def count = 1
        for (tx in block['tx']) {
            def txid = tx

            result = bitcoinAPI.getRawTransaction(txid)
            rawtransactions.add(result)

            count++

            if (testMode && count > 10) {
                break
            }
        }

        // Iterate through each raw transaction and get the parsed transaction by calling decoderawtransaction
        def parsedTransactions = []
        for (rawTransaction in rawtransactions) {
            def notCounterwalletSend = false
            def inputAddresses = []
            def outputAddresses = []
            def amounts = [] //same position as address in outputAddresses
            def Long inAmount = 0
            def Long outAmount = 0
            def decodedTransaction = bitcoinAPI.getTransaction(rawTransaction)
            def txid = decodedTransaction.txid
            def serviceAddress = "" // the service address which was sent to

            // Add output addresses
            for (vout in decodedTransaction.vout) {
                def address
                if (vout.scriptPubKey.addresses != null) {
                    address = vout.scriptPubKey.addresses[0]
                    def amount = vout.value
                    outputAddresses.add(address)
                    amounts.add(amount)
                }
                else {
                    outputAddresses.add("Unable to decode address")
                    amounts.add(0)
                }
            }

            // Check if the send was performed to the central service listen to any of the central asset addresses we are listening on
            def found = false
            if (outputAddresses.contains(listenerAddress)) {
                found = true
                serviceAddress = listenerAddress
            }

            // Check if a send was performed to an address registered via the API
            def counter = 0
            while (found == false && counter <= outputAddresses.size()) {
                def row = db.firstRow("select * from addressMaps where nativePaymentAddress = ${outputAddresses[counter]}")
                if (row != null) {
                    found = true
                    serviceAddress = row.nativePaymentAddress
                }

                counter++
            }

            // Record the send
            if (found == true) {
                def listenerAddressIndex = outputAddresses.findIndexOf {it == serviceAddress}
                inAmount = amounts[listenerAddressIndex] * satoshi
                assert inAmount > 0


                // Calculate fee
                def amountMinusTX
                def calculatedFee

                // Remove the TX Fee first from calculations
                amountMinusTX = inAmount - (txFee * satoshi)

                // If the amount that was sent was less than the cost of TX then eat the whole amount
                if (amountMinusTX < 0) {
                    amountMinusTX = 0
                }

                if (amountMinusTX > 0) {
                    calculatedFee = ((amountMinusTX * feeAmountPercentage / 100) + (txFee * satoshi)).toInteger()

                    if (inAmount < calculatedFee) {
                        calculatedFee = inAmount
                    }
                }
                else {
                    calculatedFee = inAmount
                }

                // Set out amount if it is more than a satoshi
                if (inAmount - calculatedFee >= 1) {
                    outAmount = inAmount - calculatedFee
                }
                else {
                    outAmount = 0
                }


                assert inAmount == outAmount + calculatedFee  // conservation of energy

                //Get input addresses
                for (vin in decodedTransaction.vin) {
                    def position = vin.vout
                    def input_txid = vin.txid

                    // If there are inputs to this tx then look them up
                    if (input_txid != null) {
                        def rawInputTransaction = bitcoinAPI.getRawTransaction(input_txid)
                        def sourceInputTransaction = bitcoinAPI.getTransaction(rawInputTransaction)
                        def inputAddress = sourceInputTransaction.vout[position].scriptPubKey.addresses[0]
                        inputAddresses.add(inputAddress)
                    }
                }

                // Check if this send was a counterwallet send
                def uniqueInputAddresses = inputAddresses.unique()
                if (uniqueInputAddresses.size() > 1) {
                    notCounterwalletSend = true
                }
                def uniqueChangeAddresses = outputAddresses.unique()
                def uniqueChangeAddressesWithoutInput = outputAddresses - uniqueInputAddresses[0] // for a counterwallet, after removing change address, send this should only leave 1 destination address
                if (uniqueChangeAddressesWithoutInput.size() > 1) {
                    notCounterwalletSend = true
                }

                // Only record if one of the input addresses is NOT the service address. ie we didn't initiate the send
                if (inputAddresses.contains(listenerAddress) == false) {
                    parsedTransactions.add([txid, inputAddresses, outputAddresses, inAmount, inAsset, outAmount, outAsset, calculatedFee, serviceAddress, notCounterwalletSend])
                    println "Block ${currentBlock} found service call: ${currentBlock} ${txid} ${inputAddresses} ${outputAddresses} ${inAmount/satoshi} ${inAsset} -> ${outAmount/satoshi} ${outAsset} (${calculatedFee/satoshi} ${inAsset} fee collected)"
                }
            }


            timeStop = System.currentTimeMillis()
        }

        // Insert into DB the transaction along with the order details
        db.execute("begin transaction")
        try {
            for (transaction in parsedTransactions) {
                def txid = transaction[0]
                def inputAddress = transaction[1][0]            // pick the first input address if there is more than 1
                def String serviceAddress = transaction[8]  // take the service address as the address that was sent to
                def inAmount = transaction[3]
                def inAsset = transaction[4]
                def outAmount = transaction[5]
                def outAsset = transaction[6]
                def feeAmount = transaction[7]
                def notCounterwalletSend = transaction[9]
                def feeAsset = inAsset

                // there will only be 1 output for counterparty assets but not the case for native assets - ie change
                // form a payment object which will determine the payment direction and source and destination addresses
                // public Payment(String inAssetValue, Long currentBlockValue, String txidValue, String sourceAddressValue, String destinationAddressValue, String outAssetValue, Long outAmountValue, Long lastModifiedBlockIdValue, Long originalAmount)
                def payment = new Payment(inAsset, currentBlock, txid, inputAddress, serviceAddress, outAsset, outAmount, currentBlock, inAmount, notCounterwalletSend)

                println "insert into transactions values (${currentBlock}, ${txid})"
                db.execute("insert into transactions values (${currentBlock}, ${txid})")
                for (eachInputAddress in transaction[1]) {
                    println "insert into inputAddresses values (${txid}, ${eachInputAddress})"
                    db.execute("insert into inputAddresses values (${txid}, ${eachInputAddress})")
                }
                for (outputAddress in transaction[2]) {
                    println "insert into outputAddresses values (${txid}, ${outputAddress})"
                    db.execute("insert into outputAddresses values (${txid}, ${outputAddress})")
                }
                println "insert into credits values (${currentBlock}, ${txid}, ${inputAddress}, '', ${inAsset}, ${inAmount}, ${outAsset}, ${outAmount}, 'valid')"
                db.execute("insert into credits values (${currentBlock}, ${txid}, ${inputAddress}, '', ${inAsset}, ${inAmount}, ${outAsset}, ${outAmount}, 'valid')")
                println "insert into fees values (${currentBlock}, ${txid}, ${feeAsset}, ${feeAmount})"
                db.execute("insert into fees values (${currentBlock}, ${txid}, ${feeAsset}, ${feeAmount})")
                if (payment.outAmount > 0) {
                    if (outAssetIssuanceDependent) {
                        // create table if not exists issuances(blockId integer, sourceTxid string, sourceAddress string, asset string, amount integer, divisibility string, status string, lastUpdatedBlockId integer)
                        println "insert into issuances values (${currentBlock}, ${txid}, ${inputAddress}, ${outAsset}, ${payment.outAmount}, ${outAssetDivisible}, ${payment.issuanceStatus}, ${currentBlock})"
                        db.execute("insert into issuances values (${currentBlock}, ${txid}, ${inputAddress}, ${outAsset}, ${payment.outAmount}, ${outAssetDivisible}, ${payment.issuanceStatus}, ${currentBlock})")
                    }

                    println "insert into payments values (${payment.currentBlock}, ${payment.txid}, ${payment.sourceAddress}, ${payment.destinationAddress}, ${payment.outAsset}, ${payment.outAmount}, ${payment.status}, ${payment.lastModifiedBlockId})"
                    db.execute("insert into payments values (${payment.currentBlock}, ${payment.txid}, ${payment.sourceAddress}, ${payment.destinationAddress}, ${payment.outAsset}, ${payment.outAmount}, ${payment.status}, ${payment.lastModifiedBlockId})")

                    // process a refund
                    if (payment.refundAmount > 0) {
                        println "insert into payments values (${payment.currentBlock}, ${payment.txid}, ${payment.sourceAddress}, ${payment.destinationAddress}, ${payment.inAsset}, ${payment.refundAmount}, ${payment.status}, ${payment.lastModifiedBlockId}) -- refund"
                        db.execute("insert into payments values (${payment.currentBlock}, ${payment.txid}, ${payment.sourceAddress}, ${payment.destinationAddress}, ${payment.inAsset}, ${payment.refundAmount}, ${payment.status}, ${payment.lastModifiedBlockId})")
                    }
                }
            }
            db.execute("commit transaction")
        } catch (Throwable e) {
            db.execute("update blocks set status='error', duration=0 where blockId = ${currentBlock}")
            println "Block ${currentBlock}: error"
            println "Exception: ${e}"
            db.execute("rollback transaction")
            assert e == null
        }

        def duration = (timeStop-timeStart)/1000
        db.execute("update blocks set status='processed', duration=${duration} where blockId = ${currentBlock}")
        println "Block ${currentBlock}: processed in ${duration}s"
    }


    public static int main(String[] args) {
        def venndNativeFollower = new VenndNativeFollower()

        venndNativeFollower.init()
        venndNativeFollower.audit()

        println "native API daemon follower started"
        println "Last processed block: " + lastProcessedBlock()
        println "Last seen block: " + lastBlock()

        // Begin following blocks
        while (true) {
            def blockHeight = bitcoinAPI.getBlockHeight()
            def currentBlock = lastBlock()
            def currentProcessedBlock = lastProcessedBlock()

            // If the current block is less than the last block we've seen then add it to the blocks db
            while (lastBlock() < blockHeight) {
                currentBlock++

                processSeenBlock(currentBlock)

                currentBlock = lastBlock() // this value should stay the same
            }

            // Check if we can process a block
            while (lastProcessedBlock() < currentBlock - confirmationsRequired) {
                currentProcessedBlock++

                processBlock(currentProcessedBlock)

                currentProcessedBlock = lastProcessedBlock()
            }

            sleep(sleepIntervalms)
        }


    } // end main
} // end class