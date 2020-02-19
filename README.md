************* Bonial Coding Task *************

    Write a lambda function which run over Lambda in order to calculate the number of passengers that each transport type is able to mobilize on each file,
________________________________________________________
Input:

S3Event trigger when new record file(below json) gets uploaded to S3 bucket:
    
    {
    "transports": [
    {"model":"Boeing 777","b-passenger-capacity":14,"e-passenger-capacity":300},
    
    {"manufacturer":"BMW","model":"M3","passenger-capacity":4}, {"model":"ICE","number-
    wagons":5,"w-passenger-capacity":30}, {"manufacturer":"Mercedes-Benz","model":"C-
    Klasse","passenger-capacity":4}, {"model":"Boeing 777S","b-passenger-capacity":10,"e-passenger-
    capacity":200}, {"manufacturer":"Audi","model":"Q3","passenger-capacity":6}
    
    ] }

Output:

Summary file(below json) should get uploaded in the same S3 bucket:
    
    {
    "planes" : 524
    "trains" : 150 "cars" : 14
    }
________________________________________________________

Prerequisites:

	1. Docker installed
	2. Files at root of this project:
        - 1.json - record json which is used to generate s3-event.json (event json)
        - s3-event.json - event json with s3 and object details
        - s3_lambda_bonial.yaml - yml describing lambda function
        - package.json - for npm to install aws sam cli locally

________________________________________________________
Test Locally:

	1. Create the JAR file: mvn package

	2. Install SAM Local: npm install -g aws-sam-local
	
	3.(optional) Create S3 bucket: aws s3api create-bucket --bucket bonial-bucket --region eu-central-1

	4. Download lambda local package globally: npm install -g lambda-local
	
	5. Run function locally by passing the s3-event.json file(which gets generated when any json file uploaded in the bucket at records folder)
		sam local invoke --template s3_lambda_bonial.yaml --event s3-event.json
	
	This will print logs including summary json

________________________________________________________
JavaDoc can be found at:

    \transport-aws-lambda-s3\javadoc\index.html