const { CosmosClient } = require("@azure/cosmos");
require("dotenv").config();

const endpoint = process.env.COSMOS_DB_ENDPOINT;
const key = process.env.COSMOS_DB_KEY;
const databaseId = process.env.COSMOS_DB_DATABASE_ID;
const containerId = process.env.COSMOS_DB_CONTAINER_ID;
const leaseContainerId = process.env.COSMOS_DB_LEASE_CONTAINER_ID;

const client = new CosmosClient({ endpoint, key });

async function initializeChangeFeed() {
  const database = client.database(databaseId);

  // Ensure lease container exists (for tracking change feed processing state)
  const leaseContainer = database.container(leaseContainerId);
  await leaseContainer.read().catch(async (error) => {
    if (error.code === 404) {
      await database.containers.createIfNotExists({ id: leaseContainerId });
      console.log(`Lease container created with id: ${leaseContainerId}`);
    } else {
      throw error;
    }
  });

  // Source container for which to monitor changes
  const container = database.container(containerId);

  // Start listening to the change feed
  const changeFeedProcessor = container.items.changeFeedProcessor("MyChangeFeedProcessor", leaseContainer, {
    startFromBeginning: true, // Start from the beginning of the change feed
    onChanges: async (changes) => {
      console.log("Detected changes:");
      changes.forEach((change) => {
        console.log(JSON.stringify(change)); // Process each change
      });
    },
    onError: (error) => {
      console.error("Error in change feed processing:", error);
    },
  });

  // Start the change feed processor
  await changeFeedProcessor.start();
  console.log("Change feed processor started. Listening for changes...");
}

initializeChangeFeed().catch((error) => {
  console.error("Error initializing change feed:", error);
});
