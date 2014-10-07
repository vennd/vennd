Vennd
=====

Vennd is a general purpose digital vending machine. Send Bitcoins to the digital vending machine and it will send back the corresponding Counterparty token. Vennd can be configured as a gateway to enable trading on the Counterparty decentralized exchange for any Bitcoin compatible alt-coin.

Feel free to contact me jeremy@vennd.io on just about anything.

The components of Vennd:
* VenndNativeFollower - Application which connects to single bitcoind instance
* CounterpartyFollower - Application which connects to the Counterparty API. A single CounterpartyFollower instance can be configured to listen to multiple service addresses and assets
* PaymentProcessor - Application to process Bitcoin and Counterparty payments
* PaymentAuthorizer - TBA - Application to manually authorize payments

For more information on how Vennd can be used, head over to vennd.io.

Pre-requisites
==============
* Bitcoin installed and blockchain synchronised with txindex=1 as per http://counterpartyd-build.readthedocs.org/en/latest/SettingUpBitcoind.html
* Counterpartyd installed as per http://counterpartyd-build.readthedocs.org/en/latest/BuildingFromSource.html#on-linux


Requirements
============
Vennd was developed on Ubuntu 12.04 LTS and is the preferred platform to run Vennd. However, Windows 7 or higher is also supported. The following are the recommended specifications for a server running Vennd:

* Ubuntu 12.04 LTS or Windows 7 or higher
* 4 GB of RAM or higher, SSD with at least 128GB free space
* Groovy 1.89 or higher. Groovy 2.3 is recommended because of JSON parsing performance improvements. It is recommended to run Vennd with the --indy parameter to enable InvokeDynamic support (http://groovy.codehaus.org/InvokeDynamic+support).
* Oracle JDK 1.7 or higher


Installation
============
The Linux installation guide can be found at https://github.com/whoisjeremylam/vennd/blob/master/doc/linux_install.md

The Windows installation guide can be found at https://github.com/whoisjeremylam/vennd/blob/master/doc/windows_install.md


Configuration
=============
A sample configuration to configure Vennd as gateway for your Bitcoin compatible alt-coin https://github.com/whoisjeremylam/vennd/blob/master/doc/gateway_configuration_guide.md

A sample configuration to configure Vennd as a simple vending machine
https://github.com/whoisjeremylam/vennd/blob/master/doc/vendingmachine_configuration_guide.md
