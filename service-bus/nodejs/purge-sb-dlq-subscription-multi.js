/**
 * 
 * @summary Remove a predetermined number of dead-letter messages from list of subscription
 * 
 */

/**
 * @summary Execute below commands to avoid certificate errors related "Error: self-signed certificate in certificate chain" / "SELF_SIGNED_CERT_IN_CHAIN"
 * 
 * $ echo quit | openssl s_client -showcerts -servername server-name.servicebus.windows.net -connect server-name.servicebus.windows.net:443 > ~/cert/server-name-servicebus-ca-certificate.pem
 * $ export NODE_EXTRA_CA_CERTS=~/cert/server-name-servicebus-ca-certificate.pem
 * 
 */

const { ServiceBusClient } = require("@azure/service-bus");

const connectionString = process.env.SERVICE_BUS_CONNECTION_STRING;

let subscriptionList = ["com.enodation.topic.json.1/RealTimeEvents/$deadletterqueue",
    "com.enodation.topic.json.2/RealTimeEvents/$deadletterqueue",
    "com.enodation.topic.json.3/RealTimeEvents/$deadletterqueue",
    "com.enodation.topic.json.4/RealTimeEvents/$deadletterqueue",
    "com.enodation.topic.json.5/RealTimeEvents/$deadletterqueue",
    "com.enodation.topic.json.6/RealTimeEvents/$deadletterqueue",
    "com.enodation.topic.json.7/RealTimeEvents/$deadletterqueue",
    "com.enodation.topic.json.8/RealTimeEvents/$deadletterqueue",
    "com.enodation.topic.json.9/RealTimeEvents/$deadletterqueue",
    "com.enodation.topic.json.10/RealTimeEvents/$deadletterqueue",
    "com.enodation.topic.json.11/RealTimeEvents/$deadletterqueue",
    "com.enodation.topic.json.12/RealTimeEvents/$deadletterqueue",
    "com.enodation.topic.json.13/RealTimeEvents/$deadletterqueue",
    "com.enodation.topic.json.14/RealTimeEvents/$deadletterqueue",
    "com.enodation.topic.json.15/RealTimeEvents/$deadletterqueue",
    "com.enodation.topic.json.16/RealTimeEvents/$deadletterqueue"
];

async function main() {
    const sbClient = new ServiceBusClient(connectionString);
    try {
        for (const subscriptionName of subscriptionList) {
            console.log(`======================================================================`);
            console.log(`Start: ${subscriptionName}`);
            let entity = subscriptionName.split("/");
            const receiver = sbClient.createReceiver(entity[0], entity[1] + '/' + entity[2]);
            let allMessages = [];
            while (allMessages.length < 1000) {
                const messages = await receiver.receiveMessages(10, {
                    maxWaitTimeInMs: 5 * 1000,
                });
                if (!messages.length) {
                    if (allMessages.length != 0) {
                        process.stdout.write("\n");
                    }
                    break;
                }
                allMessages.push(...messages);
                for (let message of messages) {
                    process.stdout.write(".");
                    await receiver.completeMessage(message); //delete messages
                }
            }
            console.log(`${allMessages.length} messsages deleted from ${subscriptionName}`);
            await receiver.close();
            console.log(`Complete: ${subscriptionName}`);
        }
        console.log(`======================================================================`);
    }
    finally {
        await sbClient.close();
    }
}

// call the main function
main().catch((err) => {
    console.log("Error occurred: ", err);
    process.exit(1);
});