/**
 * Created by whoisjeremylam on 3/05/14.
 */

@Grab(group='log4j', module='log4j', version='1.2.17')
@Grab(group='org.xerial', module='sqlite-jdbc', version='3.7.2')

import org.sqlite.SQLite
import groovy.sql.Sql

class GeneratePaymentAddress {
    private db
    private databaseName
    private BitcoinAPI bitcoinAPI
    private BitcoinAPI nativeAPI
    private nativeAssetName
    private counterpartyAssetName

    public GeneratePaymentAddress () {
        // Read in ini file
        def iniConfig = new ConfigSlurper().parse(new File("GeneratePaymentAddress.ini").toURL())
        nativeAssetName = iniConfig.nativeAssetName
        counterpartyAssetName = iniConfig.counterpartyAssetName
        databaseName = iniConfig.database.name

        db = Sql.newInstance("jdbc:sqlite:${databaseName}", "org.sqlite.JDBC")

        bitcoinAPI = new BitcoinAPI() // this needs to be the bitcoind that counterparty is connected to
        nativeAPI = new BitcoinAPI() // this needs to be the altcoind that you are connected to for venndNativeFollower
    }

    String[] generateAddress(String counterpartyAddress, String nativeAddress, String UDF1value, String UDF2value, String UDF3value, String UDF4value, String UDF5value) {
        assert counterpartyAddress != ""
        assert nativeAddress != ""
        assert nativeAssetName != ""
        assert counterpartyAssetName != ""

        def UDF1
        def UDF2
        def UDF3
        def UDF4
        def UDF5

        if (UDF1value.length() > 100) {
            UDF1value.substring(0,100)
        }
        else {
            UDF1 = UDF1value
        }

        if (UDF2value.length() > 100) {
            UDF2value.substring(0,100)
        }
        else {
            UDF2 = UDF2value
        }

        if (UDF3value.length() > 100) {
            UDF3value.substring(0,100)
        }
        else {
            UDF3 = UDF3value
        }


        if (UDF4value.length() > 100) {
            UDF4value.substring(0,100)
        }
        else {
            UDF4 = UDF4value
        }

        if (UDF5value.length() > 100) {
            UDF5value.substring(0,100)
        }
        else {
            UDF5 = UDF5value
        }

        // validate input addresses and UDF
        def String[] result

        try {
            db.execute("begin transaction")
            // insert user params into table first. This allows DB to handle race conditions - the DB will raise unique key constraint violation
            println("insert into addressMaps values ('','', ${nativeAddress}, ${counterpartyAddress}, ${counterpartyAssetName}, ${nativeAssetName}, ${UDF1}, ${UDF2}, ${UDF3}, ${UDF4}, ${UDF5})")
            db.execute("insert into addressMaps values ('','', ${nativeAddress}, ${counterpartyAddress}, ${counterpartyAssetName}, ${nativeAssetName}, ${UDF1}, ${UDF2}, ${UDF3}, ${UDF4}, ${UDF5})")

            def generatedCounterpartyAddress = bitcoinAPI.getNewAddress()
            def generatedNativeAddress = nativeAPI.getNewAddress()

            println("update addressMaps set counterpartyPaymentAddress = ${generatedCounterpartyAddress}, nativePaymentAddress = ${generatedNativeAddress} where externalAddress = ${nativeAddress} and counterpartyAddress = ${counterpartyAddress}")
            db.execute("update addressMaps set counterpartyPaymentAddress = ${generatedCounterpartyAddress}, nativePaymentAddress = ${generatedNativeAddress} where externalAddress = ${nativeAddress} and counterpartyAddress = ${counterpartyAddress}")

            result = [generatedNativeAddress, generatedCounterpartyAddress]
            db.execute("commit transaction")
        }
        catch(Throwable e) {
            return ['','']
        }

        return result
    }
}