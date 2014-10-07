/**
 * Created by whoisjeremylam on 18/04/14.
 */

@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7' )

import groovyx.net.http.AsyncHTTPBuilder
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

class CounterpartyAPI {
    def satoshi = 100000000
    private String counterpartyTransactionEncoding
    private String counterpartyRpcURL
    private String counterpartyRpcUser
    private String counterpartyRpcPassword
    private groovyx.net.http.AsyncHTTPBuilder counterpartyHttpAsync
    private boolean counterpartyMultisendPerBlock

    private init() {
        // Read in ini file
        def iniConfig = new ConfigSlurper().parse(new File("CounterpartyAPI.ini").toURL())

        counterpartyRpcURL = iniConfig.counterparty.rpcURL
        counterpartyRpcUser = iniConfig.counterparty.rpcUser
        counterpartyRpcPassword = iniConfig.counterparty.rpcPassword
        counterpartyTransactionEncoding = iniConfig.counterparty.counterpartyTransactionEncoding
        counterpartyMultisendPerBlock = iniConfig.counterparty.counterpartyMultisendPerBlock

        counterpartyHttpAsync = new AsyncHTTPBuilder(
                poolSize : 10,
                uri : counterpartyRpcURL,
                contentType : JSON )
        counterpartyHttpAsync.auth.basic counterpartyRpcUser, counterpartyRpcPassword

    }


    class Payload {
        def String method
        def Filters params
        def String id
        def String jsonrpc

        public Payload(methodValue, paramsValue, idValue) {
            method = methodValue
            params = paramsValue
            id = idValue
            jsonrpc = "2.0"
        }
    }

    public class Filters {
        def FilterValue[] filters

        public Filters(FilterValue filterValue) {
            filters = new FilterValue[1]
            filters[0]  = filterValue
        }

        public Filters() {
            filters = []
        }

        public Add(FilterValue filterValue) {
            filters.add(filterValue)
        }
    }

    public class FilterValue {
        def String field
        def String op
        def String value

        public FilterValue(String fieldVale, String opValue, String valueValue) {
            field = fieldVale
            op = opValue
            value = valueValue
        }
    }


    public getBalances(address) {
        def filterValue = new FilterValue('address', '==', address)
        def filters = new Filters(filterValue)
        def params = new Payload('get_balances', filters, 'id')
        def paramsJSON

        def result = counterpartyHttpAsync.request( POST, JSON) { req ->
            body = params

            println paramsJSON = new groovy.json.JsonBuilder(body)

            response.success = { resp, json ->
                if (json.containsKey("error")) {
                    println json.error
                    return json.error
                }

                return json.result
            }
        }

        assert result instanceof java.util.concurrent.Future
        while ( ! result.done ) {
            Thread.sleep(100)
        }

        //println result
        return result.get()
    }


    public getSends(Long blockId) {
        def paramsJSON

        def result = counterpartyHttpAsync.request( POST, JSON) { req ->
            def myParams = ["start_block":blockId,"end_block":blockId]
            body = [method : 'get_sends',
                    id : 'test',
                    params : myParams,
                    jsonrpc : "2.0"
            ]

//            println paramsJSON = new groovy.json.JsonBuilder(body)

            response.success = { resp, json ->
                if (json.containsKey("error")) {
                    println json.error
                    return json.error
                }

                return json.result
            }
        }

        assert result instanceof java.util.concurrent.Future
        while ( ! result.done ) {
            Thread.sleep(100)
        }

        return result.get()
    }

    public getIssuances(Long blockId) {
        def paramsJSON

        def result = counterpartyHttpAsync.request( POST, JSON) { req ->
            def myParams = ["start_block":blockId,"end_block":blockId]
            body = [method : 'get_issuances',
                    id : 'test',
                    params : myParams,
                    jsonrpc : "2.0"
            ]

//            println paramsJSON = new groovy.json.JsonBuilder(body)

            response.success = { resp, json ->
                if (json.containsKey("error")) {
                    println json.error
                    return json.error
                }

                return json.result
            }
        }

        assert result instanceof java.util.concurrent.Future
        while ( ! result.done ) {
            Thread.sleep(100)
        }

        return result.get()
    }


    public broadcastSignedTransaction(String signedTransaction, log4j) {
        try {
            def payloadJSON
            def myParams = [method: 'broadcast_tx',
                            id: 'test',
                            params: [signed_tx_hex:signedTransaction],
                            "jsonrpc": '2.0'
            ]

            payloadJSON = new groovy.json.JsonBuilder(myParams)
            log4j.info("broadcast payload: " + payloadJSON)


            def result = counterpartyHttpAsync.request(POST, JSON) { req ->
                body = myParams

                response.success = { resp, json ->
                    if (json.result == null) {
                        log4j.info("broadcast failed - null was returned")

                        assert json.result != null
                    }

                    return json.result
                }

                response.failure = { resp ->
                    log4j.info("BroadcastSignedTransaction failed")
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


    public signTransaction(String unsignedTransaction, log4j) {
        try {
            def payloadJSON
            def myParams = [method: 'sign_tx',
                            id: 'test',
                            params: [unsigned_tx_hex:unsignedTransaction],
                            jsonrpc : '2.0'
            ]

            payloadJSON = new groovy.json.JsonBuilder(myParams)
            log4j.info("sign_tx payload: " + payloadJSON)


            def result = counterpartyHttpAsync.request(POST, JSON) { req ->
                body = myParams

                response.success = { resp, json ->
                    if (json.result == null) {
                        log4j.info("sign_tx failed - null was returned")

                        assert json.result != null
                    }

                    return json.result
                }

                response.failure = { resp ->
                    log4j.info("SignTransaction failed")
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


    public createSend(sourceAddress, destinationAddress, asset, amount, fee, testMode, log4j) {
        def myParams

        if (testMode == false) {
            myParams = ["source":sourceAddress,"destination":destinationAddress,"asset":asset,"quantity":amount,"encoding":counterpartyTransactionEncoding,"allow_unconfirmed_inputs":counterpartyMultisendPerBlock,"fee":fee*satoshi]
//            myParams = [sourceAddress, destinationAddress, asset, amount, counterpartyTransactionEncoding, null, counterpartyMultisendPerBlock, null]
//            myParams = [sourceAddress, destinationAddress, asset, amount]
        }
        else {
            myParams = ["source":'12nY87y6qf4Efw5WZaTwgGeceXApRYAwC7',"destination":'142UYTzD1PLBcSsww7JxKLck871zRYG5D3',"asset":asset,"quantity":20000]  // in test mode send only just enough for dust
        }

        def result = counterpartyHttpAsync.request( POST, JSON) { req ->
            def payloadJSON
            def payload = [method : 'create_send',
                           id : 'test',
                           params : myParams,
                           jsonrpc : "2.0"
            ]
            body = payload

            payloadJSON = new groovy.json.JsonBuilder(body)
            log4j.info("create_send payload: " + payloadJSON)

            response.success = { resp, json ->
                if (json.containsKey("error")) {
                    return json.error
                }

                return json.result
            }

            response.failure = { resp ->
                println resp
            }
        }

        assert result instanceof java.util.concurrent.Future
        while ( ! result.done ) {
            Thread.sleep(100)
        }

        return  result.get()
    }


    public createBroadcast(String sourceAddress, BigDecimal feeFraction, String text, int timestamp, BigDecimal value, log4j) {
        try {
            def payloadJSON
            def myParams = [method: 'create_broadcast',
                            id: 'test',
                            params: [sourceAddress, feeFraction, text, timestamp, value]
            ]

            payloadJSON = new groovy.json.JsonBuilder(myParams)
            log4j.info("create_broadcast payload: " + payloadJSON)


            def result = counterpartyHttpAsync.request(POST, JSON) { req ->
                body = myParams

                response.success = { resp, json ->
                    if (json.result == null) {
                        log4j.info("CreateBroadcast failed - null was returned")

                        assert json.result != null
                    }

                    return json.result
                }

                response.failure = { resp ->
                    log4j.info("CreateBroadcast failed")
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

//    create_issuance(source, asset, quantity, divisible, description
    public createIssuance(sourceAddress, asset, quantity, divisible, description, log4j) {
        def myParams
        myParams = [sourceAddress, asset, quantity, divisible, description]

        def result = counterpartyHttpAsync.request(POST, JSON) { req ->
            def payloadJSON
            def payload = [method : 'create_issuance',
                           id: 'test',
                           params : myParams,
                           jsonrpc: "2.0"
            ]
            body = payload

            payloadJSON = new groovy.json.JsonBuilder(body)
            log4j.info("create_send payload: " + payloadJSON)

            response.success = { resp, json ->
                if (json.containsKey("error")) {
                    return json.error
                }

                return json.result
            }

            response.failure = { resp ->
                println resp
            }
        }

        assert result instanceof java.util.concurrent.Future
        while (!result.done) {
            Thread.sleep(100)
        }

        return result.get()
    }


    public CounterpartyAPI() {
        init()
    }

}
