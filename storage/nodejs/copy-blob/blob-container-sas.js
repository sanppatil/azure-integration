const {
    generateBlobSASQueryParameters,
    StorageSharedKeyCredential,
    BlobServiceClient
  } = require('@azure/storage-blob');

// Create a service SAS for a blob
function getBlobSasUri(containerClient, blobName, sharedKeyCredential, storedPolicyName) {
    const sasOptions = {
        containerName: containerClient.containerName,
        blobName: blobName
    };

    if (storedPolicyName == null) {
        sasOptions.startsOn = new Date();
        sasOptions.expiresOn = new Date(new Date().valueOf() + 3600 * 1000);
        sasOptions.permissions = BlobSASPermissions.parse("r");
    } else {
        sasOptions.identifier = storedPolicyName;
    }

    const sasToken = generateBlobSASQueryParameters(sasOptions, sharedKeyCredential).toString();
    console.log(`SAS token for blob is: ${sasToken}`);

    return `${containerClient.getBlockBlobClient(blobName).url}?${sasToken}`;
}

const sourceConnStr = process.env.AZURE_STORAGE_ACCOUNT_SOURCE_CONNECTION_STRING;
const sourceBlobServiceClient = BlobServiceClient.fromConnectionString(sourceConnStr);
const sourceContainerClient = sourceBlobServiceClient.getContainerClient("source-cont")
var blobSAS = getBlobSasUri(sourceContainerClient, "1mb.txt", sourceBlobServiceClient.credential, "test-policy");
console.log(blobSAS);