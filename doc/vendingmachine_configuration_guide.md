Vending Machine
===============

**Create Your Token on Counterparty**

Prior to creating your vending machine, first issue an asset in Counterparty. Please note if your asset is divisible or indivisible. This is required to be specified in the configuration files.

https://www.youtube.com/watch?v=eyzA5Lj1ajM


Each of the following INI files must be edited. Included below is a sample configuration for a simple vending machine which dispenses 1000 MYCOIN for each Bitcoin that is received. The following processes must be configured and running:

1) VenndNativeFollower - This process listens for BTC to be sent to the listener address of the vending machine

2) PaymentProcessor - This process sends the MYCOIN Counterparty asset

**VenndNativeFollower.ini**

Configure VenndNativeFollower to connect to bitcoind. It 'listens' for payments to the listener address.

```ini
testMode = false

inceptionBlock = 300043         // The block to start listening for service requests -1. ie start listening on block 300044
feeAmountPercentage = 0.5       // Fee amount in percentage for the vending machine to take. eg 0.5%
txFee = 0.0004172               // Fee in absolute value to take for the costs of Counterparty transaction transmission
refundTxFee = 0.0001            // TX fee in bitcoin to return funds to sender
confirmationsRequired = 3

listenerAddress = "1M72Sfpbz1BPpXFHz9m3CdqATR44Jvaydd"    // The address which the gateway will receive the native alt-coin
paymentAddress = "1BTCorgHwCg6u2YSAWKgS17qUad6kHmtQW"     // The address which the gateway will dispense the Counterparty asset
outAssetDivisible = false                                 // Defines if the output asset from the vending machine is divisible
outAssetIssuanceDependent = false
outAssetNonDivisibleRoundRule = round // floor, round, ceiling, roundRefund (round and refund if the refund amount is less than TX fee)
outAssetMultiplier = 1000                                 // 1000 MYCOIN is dispensed for each BTC that is received


inAssetName = "BTC"                                       // BTC is a reserved word for bitcoin
outAssetName = "MYCOIN"                                // This must match the name of the asset you created in Counterparty

database.name = "mycoin.db"                           // The filename of the embedded SQL database

sleepIntervalms = 1000                                    // Interval to sleep between checking for new blocks
```


**BitcoinAPI.ini**

This contains connection information to connect to the bitcoind API. Please refer to the bitcoin.conf file.
```ini
rpcURL = "http://127.0.0.1:8332"
rpcUser = "rpc"
rpcPassword = "supernova"
```


**CounterpartyAPI.ini**

Connection details to connect to counterpartyd. Please check your counterpartyd.conf file.
```ini
counterparty.rpcURL = "http://127.0.0.1:4000"
counterparty.rpcUser = "xcpRpc"
counterparty.rpcPassword = "NYNEX"
counterparty.counterpartyTransactionEncoding = "multisig"                     // Encoding scheme for Counterparty transactions
counterparty.counterpartyMultisendPerBlock = true                             // Enable more than 1 tx per block
```


**PaymentProcessor.ini**
```ini
testMode = false

bitcoin.walletPassphrase = "Imdyingtocatchmybreath"                           // The wallet password currently needs to be the same for bitcoin and the alt-coin
walletUnlockSeconds = 30

database.name = "mycoin.db"
database.busyTimeout = 2000

sleepIntervalms = 60000
```

**Start Vennd on Linux**
If you using the default installation of Groovy and followed the Linux installation instructions, the vending machine can be started by running the following commands in a Linux terminal
```sh
cd ~/Vennd
groovy VenndNativeFollower.groovy
groovy PaymentProcessor.groovy
```
or if you installed the latest version of Groovy manually

```sh
cd ~/Vennd
../groovy-binary-2.3.2/groovy VenndNativeFollower.groovy
../groovy-binary-2.3.2/groovy PaymentProcessor.groovy
```

