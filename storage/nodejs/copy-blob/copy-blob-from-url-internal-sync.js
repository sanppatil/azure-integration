const {
    BlobServiceClient,
    BlockBlobClient,
    BlobSASPermissions,
    generateBlobSASQueryParameters,
} = require('@azure/storage-blob');

var sourceConnStr = process.env.AZURE_STORAGE_ACCOUNT_SOURCE_CONNECTION_STRING;
//sourceConnStr = process.env.AZURE_STORAGE_ACCOUNT_ANALYTICS_SOURCE_CONNECTION_STRING
//sourceConnStr = "DefaultEndpointsProtocol=https;AccountName=jbdevbpstorage;AccountKey=TXKgbDzOziPl9RIzzwa+ZCmVsPJ/1pQw//64Sc3adeiAjqTO8jYnWKkuMMOIB7OoknYYpierb3d3+AStFAX8vg==;EndpointSuffix=core.windows.net"
var destinationConnStr = process.env.AZURE_STORAGE_ACCOUNT_ANALYTICS_DESTINATION_CONNECTION_STRING;
//destinationConnStr = process.env.AZURE_STORAGE_ACCOUNT_ANALYTICS_DESTINATION_CONNECTION_STRING;
async function main() {
    const sourceBlobServiceClient = BlobServiceClient.fromConnectionString(sourceConnStr);
    const sourceBlob = sourceBlobServiceClient
        .getContainerClient("source-cont")
        .getBlockBlobClient("5mb.txt");
    const destinationBlobServiceClient = BlobServiceClient.fromConnectionString(destinationConnStr);

    const destinationBlob = destinationBlobServiceClient
        .getContainerClient("destination-cont")
        .getBlockBlobClient("5mb.txt");

    var blobSasUrl = getBlobSasUri(sourceBlob);
    await destinationBlob.syncUploadFromURL(blobSasUrl);
}

// Create a service SAS for a blob
function getBlobSasUri(sourceBlob) {
    const sasOptions = {
        containerName: sourceBlob.containerName,
        blobName: sourceBlob.name
    };
    sasOptions.startsOn = new Date();
    sasOptions.expiresOn = new Date(new Date().valueOf() + 60 * 1000);
    sasOptions.permissions = BlobSASPermissions.parse("r");
    const sasToken = generateBlobSASQueryParameters(sasOptions, sourceBlob.credential).toString();
    return `${sourceBlob.url}?${sasToken}`;
}

main()
    .then(() => console.log('done'))
    .catch((ex) => console.log(ex.message));