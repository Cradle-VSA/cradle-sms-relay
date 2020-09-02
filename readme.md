
<h1>Abstract</h1>


The main use case for this application is to provide a relay service for the main CRADLE application and the server in case of no internet connection. The main application sends a referral via sms to a phone running this application. This application will then detect the sms and attempt to send it to the server. Usually, we can use platforms like Twilio to send sms to the server. However, Twilio does not have Ugandan phone numbers and international texts can be very expensive. Therefore to avoid expansive sms charges or any glitches within the external services, we run our own application that we are able to customize to our needs. This application will ideally run in a Healthcare facility where the internet connection is stable.

The application is designed to have independent components such as NetworkManager, Database repository, Service, Broadcast listener, Views, etc. Components should not have to communicate with each other. The only exception to this is the database since every component is either observing database changes or modifying it.

## Screenshots && Documentation
Screenshots and documentation for the code is provided in the base repository folder.

## Pre-push hooks
#### For Mac, run the following command  
  
     ln -s -f ../../hooks/pre-push.sh .git/hooks/pre-push  
  
#### For Windows, run the following command as an Admin  

     mklink .git\hooks\pre-push ..\..\hooks\pre-push.sh  

#### For PowerShel, run the following command as an Admin  
     New-Item -ItemType SymbolicLink -Path .\.git\hooks -Name pre-push -Value .\hooks\pre-push.sh
