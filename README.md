# stytch-java-magic-links

##### 1. Clone the repository.

Close this repository and navigate to the folder with the source code on your machine in a terminal window.

##### 2. Set ENV vars

Create a new `local.properties` file by running `cp local.properties.template local.properties`.

Edit the new `local.properties` file and add your `STYTCH_PROJECT_ID` and `STYTCH_PROJECT_SECRET`, which can be found in your [Stytch Project Dashboard](https://stytch.com/dashboard/api-keys).

##### 3. Add Magic Link URL

Visit https://stytch.com/dashboard/redirect-urls to add `http://localhost:3000/demo/authenticate` as a valid sign-up and login URL.

##### 4. Run the Server

Run `./gradlew run`

##### 5. Login

Visit `http://localhost:3000` and login with your email. Then check for the Stytch email and click the login or sign in button.

You should be signed in!
