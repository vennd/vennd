Linux Installation Guide
========================

Update the Ubuntu server
========================
```
sudo apt-get -y update
sudo apt-get -y install unzip
sudo apt-get -y install python-software-properties
sudo apt-get -y install git-core python3
```

Install Groovy
==============
The version of Groovy that is contained in the default repository is 1.8.6.

```
sudo apt-get install groovy
```

Or install Groovy 2.3.6 (preferred)

```
cd ~
wget http://dl.bintray.com/groovy/maven/groovy-binary-2.3.6.zip
unzip groovy-binary-2.3.6.zip
```

Download Sqlite
===============
```
cd ~
mkdir -p .groovy/lib
cd ~/.groovy/lib
wget https://bitbucket.org/xerial/sqlite-jdbc/downloads/sqlite-jdbc-3.7.2.jar
```

Install Oracle JDK 7
====================
The easiest way to install Oracle JDK 7 on Ubuntu is via a PPA repository http://www.webupd8.org/2012/01/install-oracle-java-jdk-7-in-ubuntu-via.html

```
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get -y install oracle-java7-installer
```

Download Vennd
==============

It is recommended that all of the processes for a single instance of a vending machine runs in the same directory. The sqlite database must be shared between each of the processes and the user permissions can be restricted to a single directory.

```
cd ~
git clone https://github.com/whoisjeremylam/vennd
mkdir vennd/logs
```


(Optional) Install Apache and Web Page Dependencies
===================================================
These steps are only necessary if you wish to run Vennd as a gateway between an alt-coin and Counterparty.

Sign up for an API key at reCAPTCHA. reCAPTCHA is used on the address generation web page.
https://www.google.com/recaptcha/admin#createsite

The following commands will install Apache, PHP 5 and reCAPTCHA and Httpful http://phphttpclient.com/

```
sudo apt-get -y install apache2
sudo apt-get -y install libapache2-mod-php5
wget https://recaptcha.googlecode.com/files/recaptcha-php-1.11.zip
wget http://phphttpclient.com/downloads/httpful.phar
unzip recaptcha-php-1.11.zip
sudo cp recaptchalib.php /var/www
sudo cp httpful.phar /var/www
sudo chmod +r /var/www/recaptchalib.php
sudo cp vennd/generate.html /var/www
sudo cp vennd/generate.php /var/www
```


Next Steps
==========
Configure Vennd to form a gateway between Bitcoin (or a Bitcoin API compatible alt-coin) and Counterparty https://github.com/whoisjeremylam/vennd/blob/master/doc/gateway_configuration_guide.md

or

Configure Vennd to create a vending machine
https://github.com/whoisjeremylam/vennd/blob/master/doc/vendingmachine_configuration_guide.md
