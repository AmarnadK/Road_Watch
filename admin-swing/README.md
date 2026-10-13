# Road Watch Kozhikode - Admin (Swing)

Desktop console for panchayat officials. Talks to the server over HTTPS; it never touches the database.

## Run in VS Code
1. Open the `road-watch-kozhikode` folder (the Java extension finds both Maven projects).
2. Start the server first (`server/`, `mvn spring-boot:run`).
3. Open `admin-swing/src/main/java/com/roadwatch/admin/Main.java` and click **Run** above `main`.

## Build a single runnable jar
    cd admin-swing
    mvn package
    java -jar target/road-watch-admin.jar

## Using it
- On the login screen enter the server address: `http://localhost:8080` locally, or your Railway URL.
- **Request account** lets an official choose their panchayat. The account stays inactive until the
  district admin approves it in the **Accounts** tab.
- Everyone can see every report; rows in grey belong to other panchayats and are read-only.
- The panchayat rule is enforced by the server, so it holds even if someone bypasses this app.
