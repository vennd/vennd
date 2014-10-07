import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.POST
import groovyx.net.http.AsyncHTTPBuilder

@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7' )

/**
 * Created by whoisjeremylam on 18/04/14.
 */
class BitcoinAPI {
    private String rpcURL
    private String rpcUser
    private String rpcPassword
    private groovyx.net.http.AsyncHTTPBuilder httpAsync

    public getBlockHeight() {
        def result = httpAsync.request( POST, JSON) { req ->
            body = [method : 'getblockcount',
                    id : 'test',
                    params : []
            ]

            response.success = { resp, json ->
                return json.result
            }
        }

        assert result instanceof java.util.concurrent.Future
        while ( ! result.done ) {
            Thread.sleep(100)
        }

        assert result.get() > 0
        return result.get()
    }


    public getBlockHash(block) {
        assert block != null

        def result = httpAsync.request( POST, JSON) { req ->
            body = [method : 'getblockhash',
                    id : 'test',
                    params : [block]
            ]

            response.success = { resp, json ->
                return json.result
            }
        }

        assert result instanceof java.util.concurrent.Future
        while ( ! result.done ) {
            Thread.sleep(100)
        }

        return result.get()
    }


    public getBlock(blockhash) {
        def result = httpAsync.request( POST, JSON) { req ->
            body = [method : 'getblock',
                    id : 'test',
                    params : [blockhash]
            ]

            response.success = { resp, json ->
                return json.result
            }
        }

        assert result instanceof java.util.concurrent.Future
        while ( ! result.done ) {
            Thread.sleep(100)
        }

        return result.get()
    }


    public getRawTransaction(tx_id) {
        def result = httpAsync.request( POST, JSON) { req ->
            body = [method : 'getrawtransaction',
                    id : 'test',
                    params : [tx_id]
            ]

            response.success = { resp, json ->
                return json.result
            }
        }

        assert result instanceof java.util.concurrent.Future
        while ( ! result.done ) {
            Thread.sleep(100)
        }

        return result.get()
    }


    public getTransaction(rawTransaction) {
        def result = httpAsync.request( POST, JSON) { req ->
            body = [method : 'decoderawtransaction',
                    id : 'test',
                    params : [rawTransaction]
            ]

            response.success = { resp, json ->
                return json.result
            }
        }

        assert result instanceof java.util.concurrent.Future
        while ( ! result.done ) {
            Thread.sleep(100)
        }

        return result.get()
    }

    public unlockBitcoinWallet(String walletPassword, int timeout) {
        try {
            def paramsJSON
            def myParams = [method: 'walletpassphrase',
                    id: 'test',
                    params: [walletPassword, timeout]
            ]

            def result = httpAsync.request(POST, JSON) { req ->
                body = myParams

                paramsJSON = new groovy.json.JsonBuilder(myParams)
//                log4j.debug(paramsJSON)

                response.success = { resp, json ->
                    return json.result
                }

                response.failure = { resp ->
                    println "UnlockBitcoinWallet failed"
                    assert resp.responseBase == null
                }
            }

            assert result instanceof java.util.concurrent.Future
            while (!result.done) {
                Thread.sleep(100)
            }

            return result.get()
        }
        catch (Throwable e) {
            assert e == null
        }
    }


    public lockBitcoinWallet() {
        def result = httpAsync.request( POST, JSON) { req ->
            body = [method : 'walletlock',
                    id : 'test',
                    params : []
            ]

            response.success = { resp, json ->
                return json.result
            }
        }

        assert result instanceof java.util.concurrent.Future
        while ( ! result.done ) {
            Thread.sleep(100)
        }
    }

    public getNewAddress() {
        def result = httpAsync.request( POST, JSON) { req ->
            body = [method : 'getnewaddress',
                    id : 'test',
                    params : []
            ]

            response.success = { resp, json ->
                return json.result
            }
        }

        assert result instanceof java.util.concurrent.Future
        while ( ! result.done ) {
            Thread.sleep(100)
        }

        return result.get()
    }


    private init(String iniFile) {
        // Read in ini file
        def iniConfig = new ConfigSlurper().parse(new File(iniFile).toURL())

        rpcURL = iniConfig.rpcURL
        rpcUser = iniConfig.rpcUser
        rpcPassword = iniConfig.rpcPassword

        // Init async http handler
        httpAsync = new AsyncHTTPBuilder(
                poolSize : 10,
                uri : rpcURL,
                contentType : JSON )
        httpAsync.auth.basic rpcUser, rpcPassword
    }

    public BitcoinAPI() {
        init("BitcoinAPI.ini")
    }

    public BitcoinAPI(String iniFile) {
        init(iniFile)
    }

}
