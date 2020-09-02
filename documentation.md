<h1>Activities</h1>


<h2>LauncherActivity</h2>


The application currently has 3 activities. The First activity is the `LauncherActivity` which basically checks if the user has already logged in. If not, we display the login screen (needs to be renamed to `LoginActivity`). Once the user entered valid credentials, we save the email and hash of the password and then proceed to launch MainActiivty.

<h2>MainActivity</h2>


The MainActivity’s primary tasks are to start/stop the `SmsService` and display the status of all the referrals through a `Recyclerview`. The Activity starts the service inside `onCreate()` and holds a reference to the service. When launched multiple times, the activity either creates a new instance of the service or binds to an existing one through `ServiceConnnection`. This activity also has an observer for any changes in the database. Once the activity is notified of any changes in the database, it notifies the `recyclerview` to update the view.


<h2>SettingActivity</h2>


The setting activity allows you to set preferences for resending failed referrals periodically. The setting activity changes these settings through shared preferences. The other components listen to these shared preference changes to perform certain actions. The setting page also allows the user to sign out.

<h1>Broadcast Receivers</h1>


<h2>MessageReceiver</h2>


This class implements the `BroadcastReceiver()` and listens for the incoming text messages. The `getUnsentSms()` queries the sms database to see all the messages we received while the service was not running. The receiver then sends these messages to the `SmsService` to be queued to be sent to the server. Since one single message has a maximum limit of 160 characters, a single long message is split into multiple messages and then recomposed by the sms application. However, since we are listening through the broadcast, we have to recompose the sms ourselves. The `onReceive()` function maps multiple messages to their phone numbers and then saves the referrals in the database.


<h1>Service</h1>


<h2>SmsService</h2>


When the SmsService is started, the service notification icon also appears in the notification bar. The service class also implements a shared preference listener to listen for changes in the settings such as timer for resending a failed referral. The service also has a database observer that listens for brand new referrals.

Once the service receives a referral, we attempt to send the referral to the server. Before sending the referral to the server, the `sendToServer()` makes sure the referral is in a valid format. After sending the referral to the server, SmsService sends back a confirmation message to whoever sent the referral. We then update the database with the network result.

The service also uses WorkManager (currently in a limbo state) to schedule the task for sending failed referrals periodically.

<h1>Database</h1>


The application uses the Android Room database, which is an abstraction over the SQLite database. The database has one table called `SmsReferralEntity` The entity has fields like *id*, *jsondata* (the actual referral), *timeReceived*, *isUploaded*, *phoneNumber*, *NumTriesUploaded* (number of times we tried uploading), *errorMessage*, and *deliveryReportSent* (whether we sent an acknowledgment sms to the sender).

The DaoAccess class has all the SQLite queries such as `insert, delete, getUnUploadedReferral, etc`. Since we have only a single entity, the queries themselves are fairly simple.


The database package also has `ReferralRepository`, which is the main interface for other classes to interact with the database. Here we can do whatever needs to be done with the database right before going into the database or vice versa. The `SmsService`, `MessageReceiver`, and `ReferralViewModel` have an instance of this class.

<h1>Dagger</h1>


`Dagger` is a dependency injection framework for android. The idea is to decouple class implementation from their dependencies. Dependency injection has many benefits, such as easily sharing a class in the application, better testability, etc.

You can specify the objects to inject throughout the application inside the _DataModule_ class in the dagger package. The _AppComponent_ interface inside the dagger package lets you declare all different activities and classes to inject the objects. We use _dagger_ to provide an instance of _sharedPreferences_, _ReferralRepository_, and _NetworkManager_ to whichever class may need it.

<h1>Network</h1>


<h2>NetworkManager</h2>


All the network calls are abstracted away inside the `NetworkManager`. From a user’s perspective, we essentially need to make two network calls, one to authenticate the login user, second is to upload the referral. Therefore, the NetworkManager class only has two public functions: _authenticateTheUser()_ and _uploadReferral()_.

The _uploadReferral()_ first tries to upload the patient with its reading. If that succeeds, the function lets the callback result. However, uploading an existing patient fails. In that case, we only try to upload the reading itself. Once we receive a result for the reading, we simply let the callback know of the result.


<h2>NetworkResult</h2>


NetworkResult defines all our custom classes for parsing network response as well as type alias for different types of callbacks.

<h2>VolleyRequests</h2>


This class defines all the different types of volley calls such as GET/POST JsonObject calls.

<h2>VolleyRequestQueue</h2>


This class holds an instance of *RequestQueue* to be used throughout the application.

<h1>Views</h1>


<h2>ReferralAlertDialog</h2>


This is a fairly simple view that shows you a detailed view of the referrals. This includes the entire referral text and other fields inside `SmsReferralEntity` The view also gives you the option to upload the referral manually. This view contacts `MainActivity` through a callback when needing to send a referral manually.

<h1>Firebase</h1>


The application uses firebase crash analytics, performance monitor, test lab, and firebase storage. Crash analytics is instrumental in getting a notification about recent crashes and gives information such as log, type of phone, the operating system, etc. We use the firebase test lab to run a scripted or unscripted UI test with multiple phones.

<h1>Improvements</h1>


<h2>UploadReferralWorker</h2>


This class needs quite a bit of work to be properly implemented. Perhaps change it to be a type of _[CoroutineWorker](https://developer.android.com/reference/kotlin/androidx/work/CoroutineWorker)_? The overall design is not very reliable right now so might need to be looked at from a different perspective.

<h2>Performance</h2>


The application needs unit tests as well as UI tests.

Since the application does not have that many pages, we can probably make the application much more responsive and faster by making it a single activity application. In this scenario, we would use fragments for different screens.

We can also turn on [Strictmode](https://developer.android.com/reference/android/os/StrictMode) for the debug builds to notice any disk reads on the main thread that might be slowing down the application.

We can also use android’s [memory profiler](https://developer.android.com/studio/profile/memory-profiler) or [leak canary](https://github.com/square/leakcanary) to detect memory leaks when we are starting/stopping application or service.
