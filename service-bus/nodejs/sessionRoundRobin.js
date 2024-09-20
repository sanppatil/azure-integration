/**
 * 
 * @summary Continually read through all the available sessions
 * 
 */

/**
 * @summary Execute below commands to avoid certificate errors related "Error: self-signed certificate in certificate chain" / "SELF_SIGNED_CERT_IN_CHAIN"
 * 
 * $ echo quit | openssl s_client -showcerts -servername server-name.servicebus.windows.net -connect server-name.servicebus.windows.net:443 > ~/cert/server-name-servicebus-ca-certificate.pem
 * $ export NODE_EXTRA_CA_CERTS=~/cert/server-name-servicebus-ca-certificate.pem
 * 
 */

const { ServiceBusClient, delay, isServiceBusError } = require("@azure/service-bus");
const moment = require('moment');

require("dotenv").config();

const connectionString = process.env.SERVICE_BUS_CONNECTION_STRING;
const topicName = "<<entity-name>>";
const subscriptionName = "<<subscription-name>>";

const maxSessionsToProcessSimultaneously = 8;
const sessionIdleTimeoutMs = 10 * 1000;
const delayOnErrorMs = 5 * 1000;

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
const log = (entry) => new Promise((resolve) => console.log(moment().format('YYYY-MM-DD HH:mm:ss'), entry));

const abortController = new AbortController();

// Log entry for session accepted
async function sessionAccepted(sessionId) {
  log(`[${sessionId}] will start processing...`);
}

// Message handler
async function processMessage(msg) {
  log(`[${msg.sessionId}|${msg.messageId}] received message with body `);
  await sleep(3000); // Sleep for 3 seconds, assume its work done on each message
  log(`[${msg.sessionId}|${msg.messageId}] message Processing completed...`);
}

async function processError(err, sessionId) {
  if (sessionId) {
    log(`Error when receiving messages from the session ${sessionId}: `, err);
  } else {
    log(`Error when creating the receiver for next available session`, err);
  }
}

// Log entry for session closed
async function sessionClosed(reason, sessionId) {
  log(`[${sessionId}] was closed because of ${reason}`);
}

// Timer for Session timeout
function createRefreshableTimer(timeoutMs, resolve) {
  let timer;
  return () => {
    clearTimeout(timer);
    timer = setTimeout(() => resolve(), timeoutMs);
  };
}

// Receive messages from single session
async function receiveFromNextSession(serviceBusClient) {
  let sessionReceiver;
  try {
    sessionReceiver = await serviceBusClient.acceptNextSession(topicName, subscriptionName, {
      maxAutoLockRenewalDurationInMs: sessionIdleTimeoutMs,
    });
  } catch (err) {
    if (
      isServiceBusError(err) &&
      (err.code === "SessionCannotBeLocked" || err.code === "ServiceTimeout")
    ) {
      log(`INFO: no available sessions, sleeping for ${delayOnErrorMs}`);
    } else {
      await processError(err, undefined);
    }
    await delay(delayOnErrorMs);
    return;
  }

  await sessionAccepted(sessionReceiver.sessionId);

  const sessionFullyRead = new Promise((resolveSessionAsFullyRead, rejectSessionWithError) => {
    const refreshTimer = createRefreshableTimer(sessionIdleTimeoutMs, resolveSessionAsFullyRead);
    refreshTimer();

    sessionReceiver.subscribe(
      {
        async processMessage(msg) {
          refreshTimer();
          await processMessage(msg);
        },
        async processError(args) {
          rejectSessionWithError(args.error);
        },
      },
      {
        abortSignal: abortController.signal,
      },
    );
  });

  try {
    await sessionFullyRead;
    await sessionClosed("idle_timeout", sessionReceiver.sessionId);
  } catch (err) {
    await processError(err, sessionReceiver.sessionId);
    await sessionClosed("error", sessionReceiver.sessionId);
  } finally {
    await sessionReceiver.close();
  }
}

// Listen to number of sessions... 
async function roundRobinThroughAvailableSessions() {
  const serviceBusClient = new ServiceBusClient(connectionString);
  const receiverPromises = [];
  for (let i = 0; i < maxSessionsToProcessSimultaneously; ++i) {
    receiverPromises.push(
      (async () => {
        while (!abortController.signal.aborted) {
          await receiveFromNextSession(serviceBusClient);
        }
      })(),
    );
  }

  log(`Listening for available sessions...`);
  await Promise.all(receiverPromises);

  await serviceBusClient.close();
  log(`Exiting...`);
}

// To stop the round-robin processing you can just call abortController.abort()
roundRobinThroughAvailableSessions().catch((err) =>
  log(`Session RoundRobin - Fatal error: ${err}`),
);