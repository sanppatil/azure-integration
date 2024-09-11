
/**
 * @summary Execute below commands to avoid certificate errors related "Error: self-signed certificate in certificate chain" / "SELF_SIGNED_CERT_IN_CHAIN"
 * 
 * $ echo quit | openssl s_client -showcerts -servername server-name.servicebus.windows.net -connect server-name.servicebus.windows.net:443 > ~\cert\server-name-servicebus-ca-certificate.pem
 * $ export NODE_EXTRA_CA_CERTS=~\cert\server-name-servicebus-ca-certificate.pem
 * 
 */

const { ServiceBusClient } = require("@azure/service-bus");

const connectionString = process.env.SERVICE_BUS_CONNECTION_STRING;

const topicName = "com.enodation.topic.json.1";
const subscriptionName = "RealTimeEvents";
const deadLetter = "";

async function main() {
    const sbClient = new ServiceBusClient(connectionString);
    const receiver = sbClient.createReceiver(topicName, subscriptionName + deadLetter);
    try {
        let allMessages = [];
        while (allMessages.length < 100) {
            const messages = await receiver.receiveMessages(1, {
                maxWaitTimeInMs: 5 * 60 * 1000,
            });

            if (!messages.length) {
                console.log("No more messages to receive");
                break;
            }
            console.log(`Received ${messages.length} messages`);
            allMessages.push(...messages);
            for (let message of messages) {
                console.log(`  Message: '${message.body}'`);
                await receiver.completeMessage(message);
            }
        }

        await receiver.close();
    } finally {
        await sbClient.close();
    }
}

// call the main function
main().catch((err) => {
    console.log("Error occurred: ", err);
    process.exit(1);
});