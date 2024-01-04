const { app } = require('@azure/functions');
const { BlobServiceClient } = require("@azure/storage-blob");

const sourceContainerName = process.env["SOURCE_CONTAINER_NAME"];
const targetStorageAccountConnection = process.env["TARGET_STORAGE_ACCOUNT_CONNECTION"];
const targetContainerName = process.env["TARGET_CONTAINER_NAME"];

app.storageBlob('storageBlobTrigger', {
    path: `${sourceContainerName}/{name}`,
    connection: 'SOURCE_STORAGE_ACCOUNT_CONNECTION',
    handler: async (blob, context) => {
        const blobName = context.triggerMetadata.name;
        const blobServiceClient = BlobServiceClient.fromConnectionString(targetStorageAccountConnection);
        const containerClient = blobServiceClient.getContainerClient(targetContainerName);
        const blockBlobClient = containerClient.getBlockBlobClient(blobName);
        const uploadBlobResponse = await blockBlobClient.upload(blob, blob.length);
        console.log(`Blob "${blobName}" with size ${blob.length} bytes was uploaded successfully. requestId: ${uploadBlobResponse.requestId}`);
    }
});
