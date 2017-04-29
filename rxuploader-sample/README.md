RxUploader Sample Application
=======

This sample application demonstrates how to use the RxUploader to manage and queue uploads.
The application connects to your 500px account to retrieve and upload photos. The photo uploaded to 500px will be private by default. You can change this by updating the `PRIVACY_MODE` defined in `MainActivity.java` to `PhotoPrivacy.PUBLIC`.

![](rxuploader_sample_app_demo.gif)

Configuration
=======

1. Sign up for a [500px][1] account
2. Retrieve a Consumer Key and Consumer Secret by [registering the application][2]
3. Update `Config.java` and replace the `CONSUMER_KEY` and `CONSUMER_SECRET` values you retrieved from Step 2
4. Update `Config.java` and replace the `X_AUTH_USERNAME` and `X_AUTH_PASSWORD` with your username and password
5. The photo to upload is read from the root external storage location. Push the photo to this location by opening terminal and entering `adb push <PATH TO PHOTO> /sdcard/test.jpeg`
6. The application doesn't explicitly ask for Storage permissions so you will need to enable these permissions explicitly by going to Settings -> Apps -> Sample -> Permissions -> Grant Storage permission

[1]: https://500px.com/signup
[2]: https://500px.com/settings/applications
