const {
    BlobServiceClient,
    BlockBlobClient
} = require('@azure/storage-blob');

const destinationConnStr = process.env.AZURE_STORAGE_ACCOUNT_DESTINATION_CONNECTION_STRING;

async function main() {

    const destinationBlobServiceClient = BlobServiceClient.fromConnectionString(destinationConnStr);

    const destinationBlob = destinationBlobServiceClient
        .getContainerClient("destination-cont")
        .getBlockBlobClient("1mb.csv");

    await destinationBlob.syncUploadFromURL("https://raw.githubusercontent.com/frictionlessdata/datasets/main/files/csv/1mb.csv");
}

main()
    .then(() => console.log('done'))
    .catch((ex) => console.log(ex.message));