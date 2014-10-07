/**
 * Created by whoisjeremylam on 3/06/14.
 */

import org.apache.log4j.*
import groovy.sql.Sql

@Grab(group='log4j', module='log4j', version='1.2.17')
@Grab(group='org.xerial', module='sqlite-jdbc', version='3.7.2')

class ConfigurationTester {
    static counterpartyAPI
    static bitcoinAPI

    // Vennd native follower params
    static inAsset
    static outAsset
    static inceptionBlock
    static feeAmountPercentage
    static txFee
    static refundTxFee
    static testMode
    static listenerAddress
    static paymentAddress
    static sleepIntervalms
    static databaseName
    static confirmationsRequired

    // payment processor params
    static walletPassphrase
    static paymentSleepIntervalms
    static paymentDatabaseName
    static counterpartyTransactionEncoding
    static walletUnlockSeconds

    // bitcoin api params
    static rpcURL
    static rpcUser
    static rpcPassword

    // counterparty api params
    static counterpartyRpcURL
    static counterpartyRpcUser
    static counterpartyRpcPassword
    static counterpartyTransactionEncoding2
    static counterpartyMultisendPerBlock

    public init() {
        counterpartyAPI = new CounterpartyAPI()
        bitcoinAPI = new BitcoinAPI()

        def venndNativeFollowerIniConfig = new ConfigSlurper().parse(new File("VenndNativeFollower.ini").toURL())
        inAsset = venndNativeFollowerIniConfig.inAssetName
        outAsset = venndNativeFollowerIniConfig.outAssetName
        inceptionBlock = venndNativeFollowerIniConfig.inceptionBlock
        feeAmountPercentage = venndNativeFollowerIniConfig.feeAmountPercentage
        txFee = venndNativeFollowerIniConfig.txFee
        refundTxFee = venndNativeFollowerIniConfig.refundTxFee
        testMode = venndNativeFollowerIniConfig.testMode
        listenerAddress = venndNativeFollowerIniConfig.listenerAddress
        paymentAddress = venndNativeFollowerIniConfig.paymentAddress
        sleepIntervalms = venndNativeFollowerIniConfig.sleepIntervalms
        databaseName = venndNativeFollowerIniConfig.database.name
        confirmationsRequired = venndNativeFollowerIniConfig.confirmationsRequired

        def paymentProcessorIniConfig = new ConfigSlurper().parse(new File("PaymentProcessor.ini").toURL())
        walletPassphrase = paymentProcessorIniConfig.bitcoin.walletPassphrase
        paymentSleepIntervalms = paymentProcessorIniConfig.sleepIntervalms
        paymentDatabaseName = paymentProcessorIniConfig.database.name
        counterpartyTransactionEncoding = paymentProcessorIniConfig.counterpartyTransactionEncoding
        walletUnlockSeconds = paymentProcessorIniConfig.walletUnlockSeconds

        def bitcoinAPIIniConfig = new ConfigSlurper().parse(new File("BitcoinAPI.ini").toURL())
        rpcURL = bitcoinAPIIniConfig.rpcURL
        rpcUser = bitcoinAPIIniConfig.rpcUser
        rpcPassword = bitcoinAPIIniConfig.rpcPassword

        def counterpartyIniConfig = new ConfigSlurper().parse(new File("CounterpartyAPI.ini").toURL())
        counterpartyRpcURL = counterpartyIniConfig.counterparty.rpcURL
        counterpartyRpcUser = counterpartyIniConfig.counterparty.rpcUser
        counterpartyRpcPassword = counterpartyIniConfig.counterparty.rpcPassword
        counterpartyTransactionEncoding2 = counterpartyIniConfig.counterparty.counterpartyTransactionEncoding
        counterpartyMultisendPerBlock = counterpartyIniConfig.counterparty.counterpartyMultisendPerBlock
    }


    public static main(String[] args) {
        def configurationTester = new ConfigurationTester()

        configurationTester.init()

        println "Vennd parameters"
        println "Test mode: ${testMode}"
        println "Inbound asset name: ${inAsset}"
        println "Outbound asset name: ${outAsset}"
        println "Starting block: ${inceptionBlock + 1}"
        println "Number of confirmations required for inbound sends: ${confirmationsRequired}"
        println "Transaction fee: ${feeAmountPercentage}%"
        println "Tx fee: ${txFee} ${inAsset}"
        println "Refund tx fee: ${refundTxFee} ${inAsset}"
        println "Listener address where ${inAsset} will be received : ${listenerAddress}"
        println "Payment address where ${outAsset} will be sent from: ${paymentAddress}"
        println "Interval to check for new blocks: ${sleepIntervalms} ms"
        println "Database name: ${databaseName}"
        println ""
        println "Payment parameters"
        println "Wallet passphrase: ${walletUnlockSeconds}"
        println "Interval to check for new payments to make: ${paymentSleepIntervalms} ms"
        println "Database name: ${paymentDatabaseName}"
        println "Counterparty tx encoding scheme: ${counterpartyTransactionEncoding}"
        println "Duration to unlock Bitcoin wallet when making payments: ${walletUnlockSeconds} secs"
        println ""
        print "Checking connection to Bitcoin api..."
        def bitcoinTest = new BitcoinAPI()
        try {
            bitcoinTest.getBlockHeight()
            println "Ok!"
        }
        catch (Throwable e) {
            println e.getMessage()
        }
        print "Unlocking Bitcoin wallet..."
        try {
            bitcoinTest.lockBitcoinWallet()
            bitcoinTest.unlockBitcoinWallet(walletPassphrase, walletUnlockSeconds)
            bitcoinTest.lockBitcoinWallet()
            println "Ok!"
        }
        catch (Throwable e) {
            println e.getMessage()
        }
        print "Checking connection to Counterparty api..."
        def counterpartyTest = new CounterpartyAPI()
        def assetBalance
        try {
            assetBalance =  counterpartyTest.getBalances(paymentAddress)
	    println assetBalance
            println "Ok!"
        }
        catch (Throwable e) {
            println e.getMessage()
        }

        if (databaseName != paymentDatabaseName) {
            println "Warning - The database names in VenndNativeFollower.ini and PaymentProcessor.ini doesn't match"
        }

	print "Connecting to DB..."
	try {
        	def db = Sql.newInstance("jdbc:sqlite:${databaseName}", "org.sqlite.JDBC")
        	db.execute("PRAGMA busy_timeout = 1000")
		println "Ok!"
	}
	catch (Throwable e) {
		println e.getMessage()
	}

	print "Initialising logger..."
	try {
        	def logger = new Logger()
        	PropertyConfigurator.configure("VenndNativeFollower_log4j.properties")
        	def log4j = logger.getRootLogger()
		println "Ok!"
	}
        catch (Throwable e) {
                println e.getMessage()
        }

        System.exit(0)
    }
}
