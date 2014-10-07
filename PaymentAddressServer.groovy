/**
 * Created by whoisjeremylam on 12/05/14.
 *
 * Thanks to https://bitbucket.org/jsumners/restlet-2.1-demo for a sane example using Restlet
 */
import groovy.json.JsonSlurper
import org.restlet.Component
import org.restlet.data.Protocol
import org.restlet.data.Status
import org.restlet.resource.Put
import org.restlet.resource.ServerResource
import org.restlet.Application
import org.restlet.Restlet
import org.restlet.routing.Router
import org.json.JSONException
import org.json.JSONObject
import org.restlet.ext.json.JsonRepresentation

@Grab(group='log4j', module='log4j', version='1.2.17')
@Grab(group='org.restlet.jse', module='org.restlet', version = '2.2.0')
@Grab(group='org.restlet.jse', module='org.restlet.ext.json', version = '2.2.0')
@Grab(group='org.restlet.jse', module='org.restlet.lib.org.json', version = '2.0') //org.restlet.jse:org.restlet.lib.org.json:2.0


class PaymentAddressServer {
    public static class PaymentAddressServerResource extends ServerResource {
        @Override
        public void doInit() {

        }


        @Put
        public String put(String value) {
            def GeneratePaymentAddress addressGenerator = new GeneratePaymentAddress()
            def JsonRepresentation jsonRepresentation
            def JSONObject jsonObject = new JSONObject()

            try {
                def slurper = new JsonSlurper()
                def result = slurper.parseText(value)
                def counterpartyAddress = result.counterpartyAddress
                def nativeRefundAddress = result.nativeRefundAddress

                if (nativeRefundAddress == null || counterpartyAddress == null) {
                    jsonObject.put("counterpartyPaymentAddress", '')
                    jsonObject.put("nativePaymentAddress", '')
                    jsonObject.put("returnCode", -1)
                }
                else {
                    def generatedAddresses = addressGenerator.generateAddress("${counterpartyAddress}", "${nativeRefundAddress}", "udf1", "udf2", "udf3", "udf4", "udf5")

                    jsonObject.put("nativePaymentAddress", generatedAddresses[0])
                    jsonObject.put("counterpartyPaymentAddress", generatedAddresses[1])
                    jsonObject.put("returnCode", 0)
                }

                jsonRepresentation = new JsonRepresentation(jsonObject)
            } catch(JSONException jsonEx) {
                System.out.println(jsonEx.toString())
            }

            response = this.getResponse()
            response.setStatus(Status.SUCCESS_CREATED)
            response.setEntity(jsonRepresentation)
        }
    }


    public static class PaymentAddressServerApplication extends Application {

        /**
         * Creates a root Restlet that will receive all incoming calls.
         */
        @Override
        public synchronized Restlet createInboundRoot() {
            // Create a router Restlet that routes each call to a new instance of HelloWorldResource.
            Router router = new Router(getContext())

            router.attach("/address", PaymentAddressServerResource.class)

            return router
        }

    }

    static init() {

    }


    public static void main(String[] args) throws Exception {
        def serverApp = new PaymentAddressServerApplication()
        init()

        // Create a new Component.
        Component component = new Component()

        // Add a new HTTP server listening on port 8182.
        component.getServers().add(Protocol.HTTP, 8182)

        // Attach the sample application.
        component.getDefaultHost().attach("/vennd", serverApp)

        // Start the component.
        component.start()
    }
}
